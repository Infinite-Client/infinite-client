package org.infinite.libs.level

import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * ワールド（Level）内の更新イベント（チャンク読み込み、ブロック更新）を管理するクラス
 */
object LevelManager {
    sealed class Chunk {
        class Data(val x: Int, val z: Int, val data: ClientboundLevelChunkPacketData) : Chunk()
        class BlockUpdate(val packet: ClientboundBlockUpdatePacket) : Chunk()
        class DeltaUpdate(val packet: ClientboundSectionBlocksUpdatePacket) : Chunk()
    }

    // キューの最大サイズ
    private const val MAX_QUEUE_SIZE = 2000

    // 現在この情報を必要としている機能があるかどうか
    @Volatile
    var hasListeners: Boolean = false

    // スレッドセーフなデックを使用
    val queue = ConcurrentLinkedDeque<Chunk>()

    /**
     * チャンクデータ受信時の処理
     */
    fun handleChunkLoad(x: Int, z: Int, chunkData: ClientboundLevelChunkPacketData) {
        if (!hasListeners) return
        addSafe(Chunk.Data(x, z, chunkData))
    }

    /**
     * 複数ブロックの更新パケットをキューに追加
     */
    fun handleDeltaUpdate(packet: ClientboundSectionBlocksUpdatePacket) {
        if (!hasListeners) return
        addSafe(Chunk.DeltaUpdate(packet))
    }

    /**
     * 単一ブロックの更新パケットをキューに追加
     */
    fun handleBlockUpdate(packet: ClientboundBlockUpdatePacket) {
        if (!hasListeners) return
        addSafe(Chunk.BlockUpdate(packet))
    }

    private fun addSafe(item: Chunk) {
        // サイズチェックと追加をアトミックに行う必要はないが、
        // 溢れそうなら古いものを消す
        while (queue.size >= MAX_QUEUE_SIZE) {
            queue.pollFirst() // 古いものを捨てる
        }
        queue.addLast(item)
    }

    fun clear() {
        queue.clear()
    }
}
