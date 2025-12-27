package org.infinite.libs.core.features.property.number

import org.infinite.libs.core.features.property.NumberProperty

// Double型（例：感度、透過率など）
class DoubleProperty(
    default: Double,
    min: Double,
    max: Double,
    suffix: String = "",
) : NumberProperty<Double>(default, min, max, suffix)
