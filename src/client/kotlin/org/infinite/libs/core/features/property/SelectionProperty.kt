package org.infinite.libs.core.features.property

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import net.minecraft.client.Minecraft
import org.infinite.libs.core.features.Property
import org.infinite.libs.ui.widgets.PropertyWidget
import org.infinite.libs.ui.widgets.SelectionPropertyWidget

/**
 * 複数の選択肢から1つを選択するプロパティ
 */
open class SelectionProperty<T : Any>(
    default: T,
    opts: List<T>,
) : Property<T>(default) {
    override fun tryApply(anyValue: Any?) {
        if (anyValue == null) return

        val foundValue: T? = when (anyValue) {
            // 1. JSON Primitive (ConfigManager からのロード時)
            is JsonPrimitive -> {
                // まずは文字列として検索を試みる
                val s = anyValue.content
                val foundByString = options.find {
                    propertyString(it).equals(s, ignoreCase = true) ||
                        it.toString().equals(s, ignoreCase = true)
                }

                if (foundByString != null) {
                    foundByString
                } else {
                    // 文字列で見つからず、かつ数値として解釈できる場合はインデックスとして試行
                    anyValue.intOrNull?.let { idx ->
                        if (idx in options.indices) options[idx] else null
                    }
                }
            }

            // 2. 文字列からの逆引き（ここが重要）
            is String -> {
                options.find {
                    // propertyString (表示名) もしくは toString (内部名) が一致するか確認
                    propertyString(it).equals(anyValue, ignoreCase = true) ||
                        it.toString().equals(anyValue, ignoreCase = true)
                }
            }

            // 3. インデックス（数値）指定
            is Number -> {
                val idx = anyValue.toInt()
                if (idx in options.indices) options[idx] else null
            }

            else -> null
        }

        if (foundValue != null) {
            this.value = foundValue
        }
    }

    open val options: List<T> = opts
    override fun filterValue(newValue: T): T {
        // 選択肢にない場合は現在の値を維持（変更を拒否）
        return if (options.contains(newValue)) newValue else value
    }

    open fun propertyString(value: T): String = value.toString()

    open fun previous() {
        val currentIndex = options.indexOf(value)
        if (currentIndex != -1) {
            value = options[(currentIndex - 1 + options.size) % options.size]
        }
    }

    /**
     * オプションの中で最も長い文字列の幅を Lazy に計算する
     */
    val minWidth: Int by lazy {
        val font = Minecraft.getInstance().font
        options.maxOfOrNull { font.width(propertyString(it)) } ?: 0
    }

    fun next() {
        val currentIndex = options.indexOf(value)
        if (currentIndex != -1) {
            value = options[(currentIndex + 1) % options.size]
        }
    }

    override fun widget(x: Int, y: Int, width: Int): PropertyWidget<SelectionProperty<T>> = SelectionPropertyWidget(x, y, width, this)
}
