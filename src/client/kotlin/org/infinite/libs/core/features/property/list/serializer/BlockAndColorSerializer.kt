package org.infinite.libs.core.features.property.list.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * JSON上で color を #AARRGGBB 文字列として扱うためのカスタムシリアライザー
 */
object BlockAndColorSerializer : KSerializer<BlockAndColor> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BlockAndColor", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BlockAndColor) {
        val colorHex = "#%08X".format(value.color)
        // 複合オブジェクトとして保存 (ConfigManagerが対応していればJsonObjectとしても可)
        val json = buildJsonObject {
            put("blockId", value.blockId)
            put("color", colorHex)
        }
        encoder.encodeSerializableValue(JsonElement.serializer(), json)
    }

    override fun deserialize(decoder: Decoder): BlockAndColor {
        val element = decoder.decodeSerializableValue(JsonElement.serializer()).jsonObject
        val blockId = element["blockId"]?.jsonPrimitive?.content ?: "minecraft:air"
        val colorStr = element["color"]?.jsonPrimitive?.content ?: "#FFFFFFFF"
        val color = try {
            colorStr.removePrefix("#").toLong(16).toInt()
        } catch (_: Exception) {
            0xFFFFFFFF.toInt()
        }
        return BlockAndColor(blockId, color)
    }
}
