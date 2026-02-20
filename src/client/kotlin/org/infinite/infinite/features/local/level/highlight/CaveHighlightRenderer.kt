package org.infinite.infinite.features.local.level.highlight

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.level.LevelManager
import org.infinite.utils.rendering.BlockMesh
import org.infinite.utils.rendering.BlockMeshGenerator
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sin

object CaveHighlightRenderer {
    private val blockPositions = ConcurrentHashMap<SectionPos, MutableMap<BlockPos, Int>>()
    private val meshCache = ConcurrentHashMap<SectionPos, BlockMesh>()
    private val sectionFirstSeen = ConcurrentHashMap<SectionPos, Long>()

    private var currentScanIndex = 0
    private var lastSettingsHash = 0
    private val colorCache = mutableMapOf<String, Int>()

    private fun getColorForBlock(id: Identifier): Int? = colorCache[id.toString()]

    fun tick(feature: CaveHighlightFeature) {
        val mc = Minecraft.getInstance()
        val world = mc.level ?: return
        val player = mc.player ?: return

        // 有効な間はリスナーを立てる
        LevelManager.hasListeners = true

        // 設定変更の検知
        val settingsHash = feature.blocksToHighlight.value.hashCode()
        if (settingsHash != lastSettingsHash) {
            clear()
            lastSettingsHash = settingsHash
            feature.blocksToHighlight.value.forEach {
                colorCache[it.blockId] = it.color
            }
        }

        // 1. LevelManager経由でパケット更新を処理 (即時性)
        processQueue(world, feature)

        // 2. 背景スキャンによる周囲の探索 (網羅性)
        val scanRadius = feature.scanRange.value
        val side = scanRadius * 2 + 1
        val center = player.chunkPosition()

        val chunksToScanPerTick = 16
        repeat(chunksToScanPerTick) {
            val ox = (currentScanIndex % side) - scanRadius
            val oz = (currentScanIndex / side) - scanRadius
            scanChunk(world, center.x + ox, center.z + oz, feature)
            currentScanIndex = (currentScanIndex + 1) % (side * side)
        }

        // 定期的に遠くのチャンクをクリーンアップ
        if (mc.player!!.tickCount % 100 == 0) {
            val toRemove = blockPositions.keys.filter {
                abs(it.x - center.x) > scanRadius + 2 || abs(it.z - center.z) > scanRadius + 2
            }
            toRemove.forEach {
                blockPositions.remove(it)
                meshCache.remove(it)
                sectionFirstSeen.remove(it)
            }
        }
    }

    private fun processQueue(world: Level, feature: CaveHighlightFeature) {
        // 1ティックあたりに処理する最大イベント数
        var processedCount = 0
        val maxProcessPerTick = if (LevelManager.queue.size > 500) 100 else 20
        val processedSections = mutableSetOf<SectionPos>()

        while (LevelManager.queue.isNotEmpty() && processedCount < maxProcessPerTick) {
            val event = LevelManager.queue.removeFirst() ?: break
            processedCount++

            when (event) {
                is LevelManager.Chunk.Data -> {
                    scanChunk(world, event.x, event.z, feature)
                }

                is LevelManager.Chunk.BlockUpdate -> {
                    val pos = event.packet.pos
                    processedSections.add(SectionPos.of(pos))
                }

                is LevelManager.Chunk.DeltaUpdate -> {
                    event.packet.runUpdates { pos, _ ->
                        processedSections.add(SectionPos.of(pos))
                    }
                }
            }
        }

        processedSections.forEach { scanSection(world, it, feature) }
    }

    private fun scanChunk(world: Level, cx: Int, cz: Int, feature: CaveHighlightFeature) {
        if (!world.chunkSource.hasChunk(cx, cz)) return
        val chunk = world.getChunk(cx, cz)

        chunk.sections.forEachIndexed { index, _ ->
            val sectionY = world.minSectionY + index
            scanSection(world, SectionPos.of(cx, sectionY, cz), feature)
        }
    }

    private fun scanSection(world: Level, sp: SectionPos, feature: CaveHighlightFeature) {
        val sectionY = sp.y
        if (sectionY * 16 > feature.maxY.value) {
            if (blockPositions.containsKey(sp)) {
                blockPositions.remove(sp)
                meshCache.remove(sp)
            }
            return
        }

        val mc = Minecraft.getInstance()
        val player = mc.player
        val playerPos = player?.blockPosition()
        val exclusionRadiusSq = feature.playerExclusionRadius.value * feature.playerExclusionRadius.value

        val newBlocks = mutableMapOf<BlockPos, Int>()
        val startX = sp.minBlockX()
        val startY = sp.minBlockY()
        val startZ = sp.minBlockZ()

        for (y in 0..15) {
            val worldY = startY + y
            if (worldY > feature.maxY.value) continue

            for (z in 0..15) {
                val worldZ = startZ + z
                for (x in 0..15) {
                    val worldX = startX + x
                    val bp = BlockPos(worldX, worldY, worldZ)

                    // プレイヤーの周囲を除外
                    if (playerPos != null && bp.distSqr(playerPos) < exclusionRadiusSq) continue

                    val state = world.getBlockState(bp)
                    val id = BuiltInRegistries.BLOCK.getKey(state.block)
                    val color = getColorForBlock(id) ?: continue

                    // Cave判定の強化: SkyLightチェック
                    if (state.isAir) {
                        val skyLight = world.getBrightness(LightLayer.SKY, bp)
                        if (skyLight > feature.skyLightThreshold.value) continue
                    }

                    newBlocks[bp] = color
                }
            }
        }

        if (blockPositions[sp] != newBlocks) {
            if (newBlocks.isEmpty()) {
                blockPositions.remove(sp)
                meshCache.remove(sp)
                sectionFirstSeen.remove(sp)
            } else {
                blockPositions[sp] = newBlocks
                meshCache.remove(sp)
                if (!sectionFirstSeen.containsKey(sp)) sectionFirstSeen[sp] = System.currentTimeMillis()
            }
        }
    }

    fun render(graphics3D: Graphics3D, feature: CaveHighlightFeature) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position()
        val lookVec = player.lookAngle
        val horizontalLook = Vec3(lookVec.x, 0.0, lookVec.z)
        val hLenSq = horizontalLook.lengthSqr()
        val renderRangeSq = (feature.renderRange.value * feature.renderRange.value).toDouble()
        val viewFocus = feature.viewFocus.value

        val meshesToDraw = mutableListOf<Triple<BlockMesh, Double, SectionPos>>()

        blockPositions.forEach { (sp, blocks) ->
            if (blocks.isEmpty()) return@forEach
            val mesh = meshCache.getOrPut(sp) { BlockMeshGenerator.generateMesh(blocks) }
            if (mesh.quads.isEmpty() && mesh.lines.isEmpty()) return@forEach

            val sectionCenter = Vec3(
                sp.minBlockX() + 8.0,
                sp.minBlockY() + 8.0,
                sp.minBlockZ() + 8.0,
            )
            val diff = sectionCenter.subtract(cameraPos)
            val distSq = diff.lengthSqr()
            if (distSq > renderRangeSq * 4) return@forEach

            val dot = if (distSq > 0.001) {
                if (hLenSq > 0.0001) {
                    lookVec.dot(diff.normalize())
                } else {
                    1.0
                }
            } else {
                1.0
            }

            val score = when (viewFocus) {
                CaveHighlightFeature.ViewFocus.Strict -> if (dot < 0.2) -1.0 else dot / (distSq + 1.0)
                CaveHighlightFeature.ViewFocus.Balanced -> (dot + 1.5) / (distSq + 1.0)
                else -> 1.0 / (distSq + 1.0)
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

            val pulse = if (feature.animation.value == CaveHighlightFeature.Animation.Pulse) (sin(time * 4.0) * 0.5 + 0.5) * 0.4 + 0.6 else 1.0
            val fadeIn = if (feature.animation.value == CaveHighlightFeature.Animation.FadeIn) ((System.currentTimeMillis() - (sectionFirstSeen[sp] ?: 0L)) / 600.0).coerceIn(0.0, 1.0) else 1.0
            val alphaMultiplier = pulse * fadeIn

            fun applyAnim(c: Int): Int {
                val a = ((c shr 24 and 0xFF) * alphaMultiplier).toInt().coerceIn(0, 255)
                return (c and 0x00FFFFFF) or (a shl 24)
            }

            if (style != CaveHighlightFeature.RenderStyle.Lines) {
                mesh.quads.forEach { q -> graphics3D.rectangleFill(q.vertex1, q.vertex2, q.vertex3, q.vertex4, applyAnim(q.color), false) }
            }
            if (style != CaveHighlightFeature.RenderStyle.Faces) {
                val width = feature.lineWidth.value
                mesh.lines.forEach { l -> graphics3D.line(l.start, l.end, applyAnim(l.color), width, false) }
            }

            drawCount += blockPositions[sp]?.size ?: 0
        }
    }

    fun clear() {
        blockPositions.clear()
        meshCache.clear()
        sectionFirstSeen.clear()
        currentScanIndex = 0

        // リスナーを下げ、キューをクリアする
        LevelManager.hasListeners = false
        LevelManager.clear()
    }
}
