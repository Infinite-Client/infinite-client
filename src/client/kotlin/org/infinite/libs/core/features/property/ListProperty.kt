package org.infinite.libs.core.features.property

import org.infinite.libs.core.features.Property
import org.infinite.libs.ui.widgets.ListPropertyWidget
import org.infinite.libs.ui.widgets.PropertyWidget

/**
 * リスト形式の設定プロパティ
 * @param T リスト内の要素の型
 * @param default デフォルトのリスト内容
 */
abstract class ListProperty<T : Any>(
    default: List<T>,
) : Property<List<T>>(default.toList()) {

    protected val internalList = java.util.concurrent.CopyOnWriteArrayList<T>(default)

    abstract fun createInputWidget(
        x: Int,
        y: Int,
        width: Int,
        initialValue: T?,
        onComplete: (T?) -> Unit,
    ): net.minecraft.client.gui.components.AbstractWidget

    open fun elementToString(element: T): String = element.toString()

    fun add(element: T) {
        internalList.add(element)
        sync()
    }

    // 番号で削除
    fun removeAt(index: Int) {
        if (index in internalList.indices) {
            internalList.removeAt(index)
            sync()
        }
    }

    // 番号で差し替え
    fun replaceAt(index: Int, newValue: T) {
        if (index in internalList.indices) {
            internalList[index] = newValue
            sync()
        }
    }

    override fun widget(x: Int, y: Int, width: Int): PropertyWidget<ListProperty<T>> =
        ListPropertyWidget(x, y, width, this)

    private fun sync() {
        value = internalList.toList()
    }
}
