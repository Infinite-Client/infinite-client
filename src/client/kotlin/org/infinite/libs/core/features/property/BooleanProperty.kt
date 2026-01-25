package org.infinite.libs.core.features.property

import org.infinite.libs.core.features.Property
import org.infinite.libs.ui.widgets.BooleanPropertyWidget
import org.infinite.libs.ui.widgets.PropertyWidget

/**
 * ON/OFFを管理するプロパティ
 * @param default デフォルト値 (true/false)
 */
class BooleanProperty(
    default: Boolean,
) : Property<Boolean>(default) {
    override fun tryApply(anyValue: Any?) {
        if (anyValue == null) return

        val converted: Boolean? = when (anyValue) {
            // 1. 直接的な Boolean
            is Boolean -> anyValue

            // 2. 数値型 (0ならfalse, それ以外はtrue)
            is Number -> anyValue.toLong() != 0L

            // 3. 文字列型 ("true" "on" "yes" なら true)
            is String -> {
                when (val s = anyValue.lowercase()) {
                    "true", "on", "yes", "1" -> true
                    "false", "off", "no", "0" -> false
                    else -> s.toBooleanStrictOrNull() // Kotlin標準のパース
                }
            }

            else -> null
        }

        if (converted != null) {
            this.value = converted
        }
    }

    /**
     * 現在の値を反転させます。
     * 親クラスの setter を通るので、スレッド安全かつ通知も飛びます。
     */
    fun toggle() {
        value = !value
    }

    override fun widget(x: Int, y: Int, width: Int): PropertyWidget<BooleanProperty> = BooleanPropertyWidget(x, y, width, this)
}
