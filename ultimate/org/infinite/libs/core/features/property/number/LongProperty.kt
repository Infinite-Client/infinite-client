package org.infinite.libs.core.features.property.number

import org.infinite.libs.core.features.property.NumberProperty

// Long型（例：シード値、キャッシュサイズなど）
class LongProperty(
    default: Long,
    min: Long,
    max: Long,
    suffix: String = "",
) : NumberProperty<Long>(default, min, max, suffix)
