package org.infinite.libs.core.features.property.list.serializer

import kotlinx.serialization.Serializable

@Serializable(with = BlockAndColorSerializer::class)
data class BlockAndColor(
    val blockId: String,
    val color: Int,
)
