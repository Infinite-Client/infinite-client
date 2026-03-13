package org.infinite.libs.core.features.property.list.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * JSON上で読み書きするためのサロゲートクラス
 */
@Serializable
private data class BlockAndColorSurrogate(
    val blockId: String,
    val color: String,
)

/**
 * JSON上で color を #AARRGGBB 文字列として扱うためのカスタムシリアライザー
 */
object BlockAndColorSerializer : KSerializer<BlockAndColor> {
    override val descriptor: SerialDescriptor = BlockAndColorSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: BlockAndColor) {
        // color を 16進数文字列 (#AARRGGBB) に変換して書き込み
        val colorHex = "#%08X".format(value.color)
        val surrogate = BlockAndColorSurrogate(value.blockId, colorHex)
        encoder.encodeSerializableValue(BlockAndColorSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): BlockAndColor {
        // JSONオブジェクトをサロゲートとして読み込み
        val surrogate = decoder.decodeSerializableValue(BlockAndColorSurrogate.serializer())

        // 16進数文字列を Int に戻す
        val color = try {
            surrogate.color.removePrefix("#").toLong(16).toInt()
        } catch (_: Exception) {
            0xFFFFFFFF.toInt() // 失敗時は白
        }

        return BlockAndColor(surrogate.blockId, color)
    }
}
