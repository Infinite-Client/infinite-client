package org.infinite.infinite.features.local.level.highlight

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.level.LevelManager
import org.infinite.utils.rendering.BlockMesh
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

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

        // --- 同期: Rust 側へデータを送る ---
        // BlockHighlight と CaveHighlight の両方のデータを Rust 側でマージして保持させるため、
        // 実際には Rust 側の Store を拡張して複数のハイライトソースを扱えるようにするのが理想的ですが、
        // 現状は BlockHighlightRenderer と CaveHighlightRenderer が交互に上書きし合わないよう、
        // Kotlin 側でデータを統合してから送る形に修正が必要です。
        // ここでは一旦、共通の sync メソッドを呼び出す形にします。
        syncWithRust(feature)
    }

    private fun syncWithRust(feature: CaveHighlightFeature) {
        val positions = mutableListOf<Int>()
        val colors = mutableListOf<Int>()

        // CaveHighlight のデータ
        blockPositions.values.forEach { map ->
            map.forEach { (pos, color) ->
                positions.add(pos.x)
                positions.add(pos.y)
                positions.add(pos.z)
                colors.add(color)
            }
        }

        // BlockHighlight のデータも追加（統合的な管理が必要）
        // ※ 簡略化のため、現在はそれぞれの Renderer が独立して Rust を呼び出しています。
        // ※ Rust 側の blocks.clear() を削除し、差分更新またはソース別管理にする必要があります。

        org.infinite.nativebind.mgpu3d.highlight.SetHighlightStyle.setHighlightStyle(
            feature.renderStyle.value.ordinal,
            feature.lineWidth.value,
        )
        org.infinite.nativebind.mgpu3d.highlight.UpdateHighlightBlocks.updateHighlightBlocks(
            "cave",
            positions.toIntArray(),
            colors.toIntArray(),
        )
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
