package org.infinite.features.rendering.search

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.dimension.DimensionType
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.world.WorldManager
import org.infinite.utils.rendering.BlockMeshGenerator
import org.infinite.utils.rendering.transparent

object BlockSearchRenderer {
    // データ構造: ブロック位置とその色 (ARGB)
    private val blockPositions = mutableMapOf<BlockPos, Int>()

    // ティックベースのスキャン状態を管理
    private const val SCAN_RADIUS_CHUNKS = 8 // プレイヤーを中心とする8チャンクの半径 (合計17x17チャンク)
    private val TOTAL_CHUNKS = (2 * SCAN_RADIUS_CHUNKS + 1).let { it * it }
    private var currentScanIndex = 0 // 現在走査中のチャンクのインデックス

    /**
     * ブロックIDに基づいて対応する色を返す。
     * BlockSearchのblockSearchColors設定から色を取得する。
     */
    private fun getColorForBlock(blockId: String): Int? {
        val blockSearchFeature = InfiniteClient.getFeature(BlockSearch::class.java)
        val blockSearchColors = blockSearchFeature?.getBlockSearchColors() ?: return null
        return blockSearchColors[blockId]?.transparent(128)
    }

    // パケットによる即時更新ロジック
    fun handleChunk(chunk: WorldManager.Chunk) {
        when (chunk) {
            is WorldManager.Chunk.Data -> {
                // チャンクロードパケットが来た場合、そのチャンクを即座にスキャン
                scanChunk(chunk.x, chunk.z)
            }

            is WorldManager.Chunk.BlockUpdate -> {
                val pos = chunk.packet.pos
                val blockState = Minecraft.getInstance().level?.getBlockState(pos)
                val blockId = blockState?.block?.let { BuiltInRegistries.BLOCK.getKey(it).toString() }
                val color = blockId?.let { getColorForBlock(it) }

                if (color != null) {
                    // 対象ブロックであれば Mapに追加/更新
                    blockPositions[pos] = color
                } else {
                    // 対象外のブロックであれば Mapから削除
                    blockPositions.remove(pos)
                }
            }

            is WorldManager.Chunk.DeltaUpdate -> {
                chunk.packet.runUpdates { pos, state ->
                    val blockId = BuiltInRegistries.BLOCK.getKey(state.block).toString()
                    val color = getColorForBlock(blockId)

                    if (color != null) {
                        blockPositions[pos] = color
                    } else {
                        blockPositions.remove(pos)
                    }
                }
            }
        }
    }

    private var currentDimension: DimensionType? = null

    /**
     * 毎ティック呼ばれる。プレイヤーを中心に、チャンクを順番に走査する (インクリメンタルスキャン)。
     */
    fun tick() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        // プレイヤーの現在地を中心とするチャンク座標
        val centerChunkX = player.chunkPosition().x
        val centerChunkZ = player.chunkPosition().z

        // 走査すべきチャンクの相対座標をインデックスから計算
        val relativeX = (currentScanIndex % (2 * SCAN_RADIUS_CHUNKS + 1)) - SCAN_RADIUS_CHUNKS
        val relativeZ = (currentScanIndex / (2 * SCAN_RADIUS_CHUNKS + 1)) - SCAN_RADIUS_CHUNKS

        // グローバルチャンク座標
        val targetChunkX = centerChunkX + relativeX
        val targetChunkZ = centerChunkZ + relativeZ

        // ターゲットチャンクをスキャン (メインスレッドで実行)
        scanChunk(targetChunkX, targetChunkZ)

        // 次のティックで次のチャンクを走査するようにインデックスを更新
        currentScanIndex = (currentScanIndex + 1) % TOTAL_CHUNKS
    }

    /**
     * 指定されたチャンク内の対象ブロックを走査し、結果を Map に追加/更新する。
     */
    private fun scanChunk(
        chunkX: Int,
        chunkZ: Int,
    ) {
        val client = Minecraft.getInstance()
        val world = client.level ?: return
        if (world.dimensionType() != currentDimension) {
            currentDimension = world.dimensionType()
            clear()
            currentScanIndex = 0
            return
        }
        // チャンクがロードされているかを確認し、取得
        val chunk: ChunkAccess? = world.getChunk(chunkX, chunkZ)

        if (chunk != null) {
            // チャンク内のすべてのセクションを走査
            for (chunkY in 0 until chunk.sections.size) {
                val section = chunk.sections[chunkY]
                if (section != null && !section.hasOnlyAir()) {
                    scanChunkSection(chunkX, chunkY, chunkZ, section, chunk.minY)
                }
            }
        }
    }

    /**
     * チャンクセクション内のブロックを走査し、Mapを更新するヘルパー関数
     */
    private fun scanChunkSection(
        chunkX: Int,
        chunkY: Int,
        chunkZ: Int,
        section: LevelChunkSection,
        minY: Int,
    ) {
        val chunkLength = 16
        // セクション内のローカル座標 (0-15) を走査
        for (y in 0 until chunkLength) {
            for (z in 0 until chunkLength) {
                for (x in 0 until chunkLength) {
                    val blockState = section.getBlockState(x, y, z)
                    val blockId = BuiltInRegistries.BLOCK.getKey(blockState.block).toString()
                    getColorForBlock(blockId)?.let { color ->
                        val blockX = (chunkX * chunkLength) + x
                        val blockY = (chunkY * chunkLength + minY) + y
                        val blockZ = (chunkZ * chunkLength) + z
                        val pos = BlockPos(blockX, blockY, blockZ)
                        blockPositions[pos] = color
                    }
                }
            }
        }
    }

    fun clear() {
        blockPositions.clear()
        currentScanIndex = 0 // スキャンインデックスもリセット
    }

    fun render(graphics3D: Graphics3D) {
        val mesh = BlockMeshGenerator.generateMesh(blockPositions)
        graphics3D.renderSolidQuads(mesh.quads, true)
        graphics3D.renderLinedLines(mesh.lines, true)
    }
}
