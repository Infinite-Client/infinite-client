package org.infinite.libs.core.features.property.number

import org.infinite.libs.core.features.property.NumberProperty

// Int型（例：描画距離、最大フレームレートなど）
class IntProperty(
    default: Int,
    min: Int,
    max: Int,
    suffix: String = "",
) : NumberProperty<Int>(default, min, max, suffix)
