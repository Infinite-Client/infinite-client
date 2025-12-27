package org.infinite.libs.core.features.property

import org.infinite.libs.core.features.Property

/**
 * ON/OFFを管理するプロパティ
 * @param default デフォルト値 (true/false)
 */
class BooleanProperty(
    default: Boolean,
) : Property<Boolean>(default) {
    fun toggle() {
        value = !value
    }
}
