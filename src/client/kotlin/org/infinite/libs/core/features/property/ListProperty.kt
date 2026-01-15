package org.infinite.libs.core.features.property

import org.infinite.libs.core.features.Property
import org.infinite.libs.graphics.Graphics2D
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
    /**
     * 外部からの入力を List<T> として適用する
     */
    override fun tryApply(anyValue: Any?) {
        if (anyValue == null) return

        val newList: List<T>? = when (anyValue) {
            // 1. すでに正しい型のリストである場合
            is List<*> -> {
                // 要素の型が一致するか、変換可能かを確認
                // ここでは abstract メソッド convertElement を呼ぶ設計にするとより堅牢です
                @Suppress("UNCHECKED_CAST")
                anyValue.mapNotNull { it?.let { element -> convertElement(element) } }
            }

            // 2. 文字列（カンマ区切りなど）からリスト化する場合（必要に応じて）
            is String -> {
                anyValue.split(",")
                    .map { it.trim() }
                    .mapNotNull { convertElement(it) }
            }

            else -> null
        }

        if (newList != null) {
            internalList.clear()
            internalList.addAll(newList)
            sync()
        }
    }

    protected abstract fun convertElement(anyValue: Any): T?

    protected val internalList = java.util.concurrent.CopyOnWriteArrayList<T>(default)

    abstract fun createInputWidget(
        x: Int,
        y: Int,
        width: Int,
        initialValue: T?,
        onComplete: (T?) -> Unit,
    ): net.minecraft.client.gui.components.AbstractWidget

    /**
     * リスト内の1つの要素を描画する
     * @param graphics2D 描画用グラフィックス
     * @param item 対象の要素
     * @param x 描画開始X座標
     * @param y 描画開始Y座標
     * @param width 割り当てられた幅
     * @param height 割り当てられた高さ
     */
    abstract fun renderElement(graphics2D: Graphics2D, item: T, x: Int, y: Int, width: Int, height: Int)

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

    override fun widget(x: Int, y: Int, width: Int): PropertyWidget<ListProperty<T>> = ListPropertyWidget(x, y, width, this)

    private fun sync() {
        value = internalList.toList()
    }
}
