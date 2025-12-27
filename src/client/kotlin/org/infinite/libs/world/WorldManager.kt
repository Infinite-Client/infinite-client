package org.infinite.libs.world

import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket

class WorldManager {
    sealed class Chunk {
        class Data(
            val x: Int,
            val z: Int,
            val data: ClientboundLevelChunkPacketData,
        ) : Chunk()

        class BlockUpdate(
            val packet: ClientboundBlockUpdatePacket,
        ) : Chunk()

        class DeltaUpdate(
            val packet: ClientboundSectionBlocksUpdatePacket,
        ) : Chunk()
    }

    val queue: ArrayDeque<Chunk> = ArrayDeque(listOf())

    /**
     * ChunkDataをキューの最後に追加します。
     * @param x チャンクのX座標
     * @param z チャンクのZ座標
     * @param chunkData チャンクデータ
     */
    fun handleChunkLoad(
        x: Int,
        z: Int,
        chunkData: ClientboundLevelChunkPacketData,
    ) {
        queue.addLast(Chunk.Data(x, z, chunkData))
    }

    /**
     * ChunkDeltaUpdateS2CPacketをキューの最後に追加します。
     * @param packet チャンクデルタ更新パケット
     */
    fun handleDeltaUpdate(packet: ClientboundSectionBlocksUpdatePacket) {
        queue.addLast(Chunk.DeltaUpdate(packet))
    }

    /**
     * BlockUpdateS2CPacketをキューの最後に追加します。
     * @param packet ブロック更新パケット
     */
    fun handleBlockUpdate(packet: ClientboundBlockUpdatePacket) {
        queue.addLast(Chunk.BlockUpdate(packet))
    }
}
