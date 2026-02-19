package org.infinite.infinite.features.local.level.highlight

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.Graphics3D
import org.infinite.utils.rendering.BlockMesh
import org.infinite.utils.rendering.BlockMeshGenerator
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sin

object BlockHighlightRenderer {
    private val blockPositions = ConcurrentHashMap<ChunkPos, MutableMap<BlockPos, Int>>()
    private val meshCache = ConcurrentHashMap<ChunkPos, BlockMesh>()
    private val chunkFirstSeen = ConcurrentHashMap<ChunkPos, Long>()

    private var currentScanIndex = 0
    private var lastSettingsHash = 0
    private val colorCache = mutableMapOf<String, Int>()

    private fun getColorForBlock(id: Identifier, feature: BlockHighlightFeature): Int? = colorCache[id.toString()]

    fun tick(feature: BlockHighlightFeature) {
        val mc = Minecraft.getInstance()
        val world = mc.level ?: return
        val player = mc.player ?: return
        val settingsHash = feature.blocksToHighlight.value.hashCode()
        if (settingsHash != lastSettingsHash) {
            clear()
            lastSettingsHash = settingsHash
            feature.blocksToHighlight.value.forEach {
                colorCache[it.blockId] = it.color
            }
        }

        val scanRadius = feature.scanRange.value
        val side = scanRadius * 2 + 1
        val center = player.chunkPosition()

        // Scan multiple chunks per tick to cover the area faster
        val chunksToScanPerTick = 16
        repeat(chunksToScanPerTick) {
            val ox = (currentScanIndex % side) - scanRadius
            val oz = (currentScanIndex / side) - scanRadius
            scanChunk(world, center.x + ox, center.z + oz, feature)
            currentScanIndex = (currentScanIndex + 1) % (side * side)
        }

        if (mc.player!!.tickCount % 100 == 0) {
            val toRemove = blockPositions.keys.filter { abs(it.x - center.x) > scanRadius + 2 || abs(it.z - center.z) > scanRadius + 2 }
            toRemove.forEach {
                blockPositions.remove(it)
                meshCache.remove(it)
                chunkFirstSeen.remove(it)
            }
        }
    }

    private fun scanChunk(world: Level, cx: Int, cz: Int, feature: BlockHighlightFeature) {
        if (!world.chunkSource.hasChunk(cx, cz)) return
        val chunk = world.getChunk(cx, cz)
        val cp = ChunkPos(cx, cz)
        val newBlocks = mutableMapOf<BlockPos, Int>()

        chunk.sections.forEachIndexed { index, section ->
            if (section == null || section.hasOnlyAir()) return@forEachIndexed
            val minY = (world.minSectionY + index) shl 4
            for (y in 0..15) {
                for (z in 0..15) {
                    for (x in 0..15) {
                        val state = section.getBlockState(x, y, z)
                        if (state.isAir) continue
                        val color = getColorForBlock(BuiltInRegistries.BLOCK.getKey(state.block), feature) ?: continue
                        newBlocks[BlockPos((cx shl 4) + x, minY + y, (cz shl 4) + z)] = color
                    }
                }
            }
        }

        if (blockPositions[cp] != newBlocks) {
            if (newBlocks.isEmpty()) {
                blockPositions.remove(cp)
                meshCache.remove(cp)
                chunkFirstSeen.remove(cp)
            } else {
                blockPositions[cp] = newBlocks
                meshCache.remove(cp)
                if (!chunkFirstSeen.containsKey(cp)) chunkFirstSeen[cp] = System.currentTimeMillis()
            }
        }
    }

    fun render(graphics3D: Graphics3D, feature: BlockHighlightFeature) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position()
        val lookVec = player.lookAngle
        val renderRangeSq = (feature.renderRange.value * feature.renderRange.value).toDouble()
        val viewFocus = feature.viewFocus.value

        val meshesToDraw = mutableListOf<Triple<BlockMesh, Double, ChunkPos>>()

        blockPositions.forEach { (cp, blocks) ->
            if (blocks.isEmpty()) return@forEach
            val mesh = meshCache.getOrPut(cp) { BlockMeshGenerator.generateMesh(blocks) }
            if (mesh.quads.isEmpty() && mesh.lines.isEmpty()) return@forEach

            val chunkMid = Vec3(cp.middleBlockX.toDouble(), cameraPos.y, cp.middleBlockZ.toDouble())
            val diff = chunkMid.subtract(cameraPos)
            val distSq = diff.lengthSqr()
            if (distSq > renderRangeSq * 4) return@forEach

            val dot = if (distSq > 0.001) {
                lookVec.dot(diff.normalize())
            } else {
                1.0 // If inside the chunk center, consider it in focus
            }

            val score = when (viewFocus) {
                BlockHighlightFeature.ViewFocus.Strict -> if (dot < 0.2) -1.0 else dot / (distSq + 1.0)
                BlockHighlightFeature.ViewFocus.Balanced -> (dot + 1.5) / (distSq + 1.0)
                else -> 1.0 / (distSq + 1.0)
            }

            if (score >= 0) meshesToDraw.add(Triple(mesh, score, cp))
        }

        meshesToDraw.sortByDescending { it.second }
        var drawCount = 0
        val maxCount = feature.maxDrawCount.value
        val style = feature.renderStyle.value
        val time = System.currentTimeMillis() / 1000.0

        meshesToDraw.forEach { (mesh, _, cp) ->
            if (drawCount > maxCount) return@forEach

            val pulse = if (feature.animation.value == BlockHighlightFeature.Animation.Pulse) (sin(time * 4.0) * 0.5 + 0.5) * 0.4 + 0.6 else 1.0
            val fadeIn = if (feature.animation.value == BlockHighlightFeature.Animation.FadeIn) ((System.currentTimeMillis() - (chunkFirstSeen[cp] ?: 0L)) / 600.0).coerceIn(0.0, 1.0) else 1.0
            val alphaMultiplier = pulse * fadeIn

            fun applyAnim(c: Int): Int {
                val a = ((c shr 24 and 0xFF) * alphaMultiplier).toInt().coerceIn(0, 255)
                return (c and 0x00FFFFFF) or (a shl 24)
            }

            if (style != BlockHighlightFeature.RenderStyle.Lines) {
                mesh.quads.forEach { q -> graphics3D.rectangleFill(q.vertex1, q.vertex2, q.vertex3, q.vertex4, applyAnim(q.color), false) }
            }
            if (style != BlockHighlightFeature.RenderStyle.Faces) {
                val width = feature.lineWidth.value
                mesh.lines.forEach { l -> graphics3D.line(l.start, l.end, applyAnim(l.color), width, false) }
            }

            // Count actual block count for budget instead of mesh complexity
            drawCount += blockPositions[cp]?.size ?: 0
        }
    }

    fun clear() {
        blockPositions.clear()
        meshCache.clear()
        chunkFirstSeen.clear()
        currentScanIndex = 0
        colorCache.clear()
    }
}
