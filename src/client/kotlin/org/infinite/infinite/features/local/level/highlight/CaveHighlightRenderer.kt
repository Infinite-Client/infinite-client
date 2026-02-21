package org.infinite.infinite.features.local.level.highlight

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.level.LevelManager
import org.infinite.nativebind.CaveHighlight
import org.infinite.utils.rendering.BlockMesh
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object CaveHighlightRenderer {
    private val blockPositions = ConcurrentHashMap<SectionPos, MutableMap<BlockPos, Int>>()
    private val meshCache = ConcurrentHashMap<SectionPos, BlockMesh>()
    private val sectionFirstSeen = ConcurrentHashMap<SectionPos, Long>()

    private val native = CaveHighlight()
    private var handlerId = -1

    private var currentScanIndex = 0
    private var lastSettingsHash = 0
    private val colorCache = mutableMapOf<String, Int>()

    private fun getColorForBlock(id: Identifier): Int? = colorCache[id.toString()]

    fun tick(feature: CaveHighlightFeature) {
        if (handlerId == -1) {
            handlerId = native.registerToMgpu3d().toInt()
        }

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

        // --- 同期: Rust 側へデータを送る ---
        syncWithRust(feature)
    }

    private fun syncWithRust(feature: CaveHighlightFeature) {
        native.clear()
        native.lineWidth = feature.lineWidth.value
        native.renderStyle = feature.renderStyle.value.ordinal

        val positions = IntArray(blockPositions.values.sumOf { it.size } * 3)
        val colors = IntArray(blockPositions.values.sumOf { it.size })
        var i = 0
        var j = 0
        blockPositions.values.forEach { map ->
            map.forEach { (pos, color) ->
                positions[i++] = pos.x
                positions[i++] = pos.y
                positions[i++] = pos.z
                colors[j++] = color
            }
        }
        native.addBlocks(positions, colors)

        native.generate()
    }

    private fun processQueue(world: Level, feature: CaveHighlightFeature) {
// ... (中略)
    }

    private fun scanChunk(world: Level, cx: Int, cz: Int, feature: CaveHighlightFeature) {
// ... (中略)
    }

    private fun scanSection(world: Level, sp: SectionPos, feature: CaveHighlightFeature) {
// ... (中略)
    }

    fun render(graphics3D: Graphics3D, feature: CaveHighlightFeature) {
        // Kotlin 側での描画ループは不要になりました。
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
