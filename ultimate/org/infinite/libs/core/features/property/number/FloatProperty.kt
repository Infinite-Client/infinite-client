package org.infinite.libs.core.features.property.number

import org.infinite.libs.core.features.property.NumberProperty

class FloatProperty(
    default: Float,
    min: Float,
    max: Float,
    suffix: String = "",
) : NumberProperty<Float>(default, min, max, suffix)
