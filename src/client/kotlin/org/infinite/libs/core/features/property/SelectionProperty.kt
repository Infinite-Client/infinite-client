package org.infinite.libs.core.features.property

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
    open val options: List<T> = opts
    override fun filterValue(newValue: T): T {
        // 選択肢にない場合は現在の値を維持（変更を拒否）
        return if (options.contains(newValue)) newValue else value
    }

    open fun propertyString(value: T): String {
        return value.toString()
    }

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

    override fun widget(x: Int, y: Int, width: Int): PropertyWidget<SelectionProperty<T>> =
        SelectionPropertyWidget(x, y, width, this)
}
