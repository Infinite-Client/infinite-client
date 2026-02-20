package org.infinite.infinite.features.local.level.highlight

import net.minecraft.client.Minecraft
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.graphics.mesh.InfiniteMesh
import org.infinite.nativebind.BlockMeshGenerator
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

object BlockHighlightRenderer {
    private val blockIdsInSection = ConcurrentHashMap<SectionPos, IntArray>()
    private val meshCache = ConcurrentHashMap<SectionPos, InfiniteMesh>()
    private val sectionFirstSeen = ConcurrentHashMap<SectionPos, Long>()

    private var scanSpiral = listOf<Pair<Int, Int>>()
    private var currentScanIndex = 0
    private var lastScanRadius = -1
    private var lastSettingsHash = 0
    private val colorCache = mutableMapOf<String, Int>()
    private var filterIds = IntArray(0)
    private var filterColors = IntArray(0)

    private fun getColorForBlock(id: Identifier, feature: BlockHighlightFeature): Int? = colorCache[id.toString()]

    private fun updateScanSpiral(radius: Int) {
        if (radius == lastScanRadius) return
        val list = mutableListOf<Pair<Int, Int>>()
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                list.add(x to z)
            }
        }
        // Sort by distance from center (0,0) to create a spiral effect
        list.sortBy { it.first * it.first + it.second * it.second }
        scanSpiral = list
        lastScanRadius = radius
        currentScanIndex = 0
    }

    fun tick(feature: BlockHighlightFeature) {
        val mc = Minecraft.getInstance()
        val world = mc.level ?: return
        val player = mc.player ?: return
        val settingsHash = feature.blocksToHighlight.value.hashCode()
        if (settingsHash != lastSettingsHash) {
            clear()
            lastSettingsHash = settingsHash

            val ids = mutableListOf<Int>()
            val colors = mutableListOf<Int>()
            val items = feature.blocksToHighlight.value
            for (item in items) {
                val blockId = Identifier.parse(item.blockId)
                val blockOptional = BuiltInRegistries.BLOCK.get(blockId)
                if (blockOptional.isPresent) {
                    val block = blockOptional.get().value()
                    val id = BuiltInRegistries.BLOCK.getId(block)
                    colorCache[item.blockId] = item.color
                    ids.add(id)
                    colors.add(item.color)
                }
            }
            filterIds = ids.toIntArray()
            filterColors = colors.toIntArray()
        }

        val scanRadius = feature.scanRange.value
        updateScanSpiral(scanRadius)

        val center = player.chunkPosition()
        val chunksToScanPerTick = 16

        if (scanSpiral.isNotEmpty()) {
            repeat(chunksToScanPerTick) {
                val offset = scanSpiral[currentScanIndex]
                scanChunk(world, center.x + offset.first, center.z + offset.second, feature)
                currentScanIndex = (currentScanIndex + 1) % scanSpiral.size
            }
        }

        if (mc.player!!.tickCount % 100 == 0) {
            val toRemove = blockIdsInSection.keys.filter { abs(it.x - center.x) > scanRadius + 2 || abs(it.z - center.z) > scanRadius + 2 }
            toRemove.forEach {
                blockIdsInSection.remove(it)
                meshCache[it]?.close()
                meshCache.remove(it)
                sectionFirstSeen.remove(it)
            }
        }
    }

    private fun scanChunk(world: Level, cx: Int, cz: Int, feature: BlockHighlightFeature) {
        if (!world.chunkSource.hasChunk(cx, cz)) return
        val chunk = world.getChunk(cx, cz)

        chunk.sections.forEachIndexed { index, section ->
            val sectionY = world.minSectionY + index
            val sp = SectionPos.of(cx, sectionY, cz)

            if (section.hasOnlyAir()) {
                if (blockIdsInSection.containsKey(sp)) {
                    blockIdsInSection.remove(sp)
                    meshCache[sp]?.close()
                    meshCache.remove(sp)
                    sectionFirstSeen.remove(sp)
                }
                return@forEachIndexed
            }

            val ids = IntArray(4096)
            var hasHighlightable = false
            for (y in 0..15) {
                for (z in 0..15) {
                    for (x in 0..15) {
                        val state = section.getBlockState(x, y, z)
                        val id = BuiltInRegistries.BLOCK.getId(state.block)
                        ids[x + (z shl 4) + (y shl 8)] = id
                        if (!hasHighlightable && filterIds.contains(id)) hasHighlightable = true
                    }
                }
            }

            if (!hasHighlightable) {
                if (blockIdsInSection.containsKey(sp)) {
                    blockIdsInSection.remove(sp)
                    meshCache[sp]?.close()
                    meshCache.remove(sp)
                    sectionFirstSeen.remove(sp)
                }
                return@forEachIndexed
            }

            val oldIds = blockIdsInSection[sp]
            if (oldIds == null || !oldIds.contentEquals(ids)) {
                blockIdsInSection[sp] = ids
                meshCache[sp]?.close()
                meshCache.remove(sp)
                if (!sectionFirstSeen.containsKey(sp)) sectionFirstSeen[sp] = System.currentTimeMillis()
            }
        }
    }

    private fun generateMesh(sp: SectionPos, ids: IntArray): InfiniteMesh {
        val generator = BlockMeshGenerator()
        generator.clear()

        generator.addBlocksWithFilter(
            sp.minBlockX(),
            sp.minBlockY(),
            sp.minBlockZ(),
            ids,
            filterIds,
            filterColors,
        )
        generator.generate()

        val mesh = InfiniteMesh()
        mesh.updateFromNativeMesh(
            generator.getLineBufferPtr(),
            generator.getLineBufferSize(),
            generator.getQuadBufferPtr(),
            generator.getQuadBufferSize(),
        )

        generator.close()
        return mesh
    }

    fun render(graphics3D: Graphics3D, feature: BlockHighlightFeature) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position()
        val lookVec = player.lookAngle
        val renderRangeSq = (feature.renderRange.value * feature.renderRange.value).toDouble()
        val viewFocus = feature.viewFocus.value

        val defaultFov = 70.0
        val currentFov = mc.options.fov().get().toDouble()
        val zoomFactor = (defaultFov / currentFov).coerceAtLeast(1.0)

        val meshesToDraw = mutableListOf<Triple<InfiniteMesh, Double, SectionPos>>()

        blockIdsInSection.forEach { (sp, ids) ->
            val mesh = meshCache.getOrPut(sp) { generateMesh(sp, ids) }

            val sectionCenter = Vec3(
                sp.minBlockX() + 8.0,
                sp.minBlockY() + 8.0,
                sp.minBlockZ() + 8.0,
            )
            val diff = sectionCenter.subtract(cameraPos)
            val distSq = diff.lengthSqr()
            if (distSq > renderRangeSq * 4) return@forEach

            val dot = if (distSq > 0.001) {
                lookVec.dot(diff.normalize())
            } else {
                1.0
            }

            // Lenient scoring for better visibility
            val focusPower = if (zoomFactor > 1.1) 1.5 * zoomFactor else 0.5
            val adjustedDot = if (dot > 0) Math.pow(dot, focusPower) else dot

            val score = when (viewFocus) {
                BlockHighlightFeature.ViewFocus.Strict -> {
                    val threshold = 0.6 / zoomFactor
                    if (dot < threshold) -1.0 else (adjustedDot + 0.3) / (sqrt(distSq) + 1.0)
                }

                BlockHighlightFeature.ViewFocus.Balanced -> {
                    (adjustedDot + 1.0) / (sqrt(distSq) + 1.0)
                }

                else -> 1.0 / (sqrt(distSq) + 1.0)
            }

            if (score >= 0) meshesToDraw.add(Triple(mesh, score, sp))
        }

        meshesToDraw.sortByDescending { it.second }
        var drawCount = 0
        val maxCount = feature.maxDrawCount.value
        val style = feature.renderStyle.value
        val time = System.currentTimeMillis() / 1000.0

        meshesToDraw.forEach { (mesh, _, sp) ->
            if (drawCount > maxCount) return@forEach

            val pulse = if (feature.animation.value == BlockHighlightFeature.Animation.Pulse) (sin(time * 4.0) * 0.5 + 0.5) * 0.4 + 0.6 else 1.0
            val fadeIn = if (feature.animation.value == BlockHighlightFeature.Animation.FadeIn) ((System.currentTimeMillis() - (sectionFirstSeen[sp] ?: 0L)) / 600.0).coerceIn(0.0, 1.0) else 1.0
            val alphaMultiplier = pulse * fadeIn

            fun applyAnim(c: Int): Int {
                val a = ((c ushr 24 and 0xFF) * alphaMultiplier).toInt().coerceIn(0, 255)
                return (c and 0x00FFFFFF) or (a shl 24)
            }

            if (style != BlockHighlightFeature.RenderStyle.Lines) {
                val buffer = mesh.getQuadBuffer()
                val size = mesh.getQuadBufferSize()
                for (i in 0 until size step 13) {
                    val v1 = Vec3(buffer.get(ValueLayout.JAVA_FLOAT, i * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 1) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 2) * 4L).toDouble())
                    val v2 = Vec3(buffer.get(ValueLayout.JAVA_FLOAT, (i + 3) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 4) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 5) * 4L).toDouble())
                    val v3 = Vec3(buffer.get(ValueLayout.JAVA_FLOAT, (i + 6) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 7) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 8) * 4L).toDouble())
                    val v4 = Vec3(buffer.get(ValueLayout.JAVA_FLOAT, (i + 9) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 10) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 11) * 4L).toDouble())
                    val color = buffer.get(ValueLayout.JAVA_INT, (i + 12) * 4L)
                    graphics3D.rectangleFill(v1, v2, v3, v4, applyAnim(color), false)
                }
            }
            if (style != BlockHighlightFeature.RenderStyle.Faces) {
                val buffer = mesh.getLineBuffer()
                val size = mesh.getLineBufferSize()
                val width = feature.lineWidth.value
                for (i in 0 until size step 7) {
                    val v1 = Vec3(buffer.get(ValueLayout.JAVA_FLOAT, i * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 1) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 2) * 4L).toDouble())
                    val v2 = Vec3(buffer.get(ValueLayout.JAVA_FLOAT, (i + 3) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 4) * 4L).toDouble(), buffer.get(ValueLayout.JAVA_FLOAT, (i + 5) * 4L).toDouble())
                    val color = buffer.get(ValueLayout.JAVA_INT, (i + 6) * 4L)
                    graphics3D.line(v1, v2, applyAnim(color), width, false)
                }
            }

            drawCount += blockIdsInSection[sp]?.size ?: 0
        }
    }

    fun clear() {
        blockIdsInSection.clear()
        meshCache.values.forEach { it.close() }
        meshCache.clear()
        sectionFirstSeen.clear()
        currentScanIndex = 0
        colorCache.clear()
        filterIds = IntArray(0)
        filterColors = IntArray(0)
    }
}
