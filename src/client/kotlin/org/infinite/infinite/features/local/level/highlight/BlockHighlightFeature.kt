package org.infinite.infinite.features.local.level.highlight

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.phys.Vec3
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.list.BlockAndColorListProperty
import org.infinite.libs.core.features.property.list.serializer.BlockAndColor
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.graphics.mesh.HighlightMeshEngine
import org.infinite.libs.log.LogSystem
import java.util.concurrent.ConcurrentLinkedQueue

class BlockHighlightFeature : LocalFeature() {
    override val featureType = FeatureLevel.Utils

    // プロパティ定義
    val blocksToHighlight by property(
        BlockAndColorListProperty(
            listOf(
                BlockAndColor("minecraft:diamond_ore", 0xFF00FFFF.toInt()),
                BlockAndColor("minecraft:gold_ore", 0xFFFFFF00.toInt()),
            ),
        ),
    )
    val highlightMode by property(EnumSelectionProperty(HighlightMeshEngine.HighlightMode.Lines))
    val scanRange by property(IntProperty(4, 1, 16, " chunks")) // チャンク単位でのスキャン範囲 (例: 4x4チャンク)

    // メッシュエンジンインスタンス (onLevelRenderingのコンテキストで初期化される)
    private lateinit var meshEngine: HighlightMeshEngine

    // プレイヤーの周りのチャンクを巡回するための状態
    private var currentScanIndex = 0

    // 発見されたブロックを一時的に保持するキュー
    private val foundBlocksQueue = ConcurrentLinkedQueue<Pair<BlockPos, Int>>()

    override fun onEndTick() {
        super.onEndTick()
        val p = player ?: return
        val l = level ?: return

        // スキャン範囲に基づいてチャンクを巡回
        val playerChunkX = p.chunkPosition().x
        val playerChunkZ = p.chunkPosition().z
        val range = scanRange.value

        val startChunkX = playerChunkX - range
        val endChunkX = playerChunkX + range
        val startChunkZ = playerChunkZ - range
        val endChunkZ = playerChunkZ + range

        // 毎ティック1つのチャンクをスキャン
        val numChunksX = endChunkX - startChunkX + 1
        val numChunksZ = endChunkZ - startChunkZ + 1
        val totalChunks = numChunksX * numChunksZ
        if (totalChunks <= 0) return

        currentScanIndex = (currentScanIndex + 1) % totalChunks

        val targetChunkOffsetX = currentScanIndex / numChunksZ
        val targetChunkOffsetZ = currentScanIndex % numChunksZ

        val targetChunkX = startChunkX + targetChunkOffsetX
        val targetChunkZ = startChunkZ + targetChunkOffsetZ

        // 指定されたチャンクがロードされているか確認
        val chunk = l.getChunk(targetChunkX, targetChunkZ)

        // チャンク内のすべてのセクションをスキャン (Y座標はLevelのminBuildHeightからmaxBuildHeightまで)
        // Minecraft 1.18+ ではY座標が負の領域も存在する
        val minSectionY = l.minSectionY
        val maxSectionY = l.maxSectionY

        for (sectionY in minSectionY..maxSectionY) {
            val chunkSection = chunk.getSection(l.getSectionIndex(sectionY))
            // セクションにデータが存在しない場合はスキップ
            if (!chunkSection.hasOnlyAir()) {
                for (x in 0..15) {
                    for (y in 0..15) {
                        for (z in 0..15) {
                            val blockState = chunkSection.getBlockState(x, y, z)
                            // 空気のブロックは無視
                            if (blockState.isAir) continue

                            val blockId = BuiltInRegistries.BLOCK.getKey(blockState.block).toString()

                            // ハイライト対象ブロックかどうかチェック
                            blocksToHighlight.value.forEach { highlightEntry ->
                                if (highlightEntry.blockId == blockId) {
                                    val actualBlockPos = BlockPos(
                                        SectionPos.sectionToBlockCoord(targetChunkX, x),
                                        SectionPos.sectionToBlockCoord(sectionY, y),
                                        SectionPos.sectionToBlockCoord(targetChunkZ, z),
                                    )
                                    foundBlocksQueue.add(actualBlockPos to highlightEntry.color)
                                    // 1つのブロックが見つかったら、このブロックのチェックは終了
                                    // (同じブロックIDに対して複数色設定がない限りはbreak可能)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onLevelRendering(graphics3D: Graphics3D) {
        LogSystem.log("RENDER")

        // onLevelRenderingが呼ばれたときにMeshEngineを初期化（Graphics3Dインスタンスが必要なため）
        // meshEngineが初期化されていない、またはGraphics3Dインスタンスが変わった場合に再初期化
        if (!::meshEngine.isInitialized) {
            meshEngine = HighlightMeshEngine(graphics3D)
        }

        // キューからブロックを取り出してメッシュエンジンに追加
        while (foundBlocksQueue.isNotEmpty()) {
            val (blockPos, color) = foundBlocksQueue.poll()
            // Vec3に変換して渡す
            meshEngine.addCube(Vec3(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble()), color)
        }

        // メッシュエンジンで描画
        meshEngine.render(highlightMode.value)
    }
}
