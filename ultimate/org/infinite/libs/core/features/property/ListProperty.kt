package org.infinite.libs.core.features.property

import org.infinite.libs.core.features.Property

/**
 * リスト形式の設定プロパティ
 * @param T リスト内の要素の型
 * @param default デフォルトのリスト内容
 */
open class ListProperty<T>(
    default: List<T>,
) : Property<List<T>>(default) {
    private val _value: MutableList<T> = default.toMutableList()

    /**
     * 現在保持しているリスト（読み取り専用）
     */
    override var value: List<T>
        get() = _value
        set(value) {
            _value.clear()
            _value.addAll(value)
        }

    /**
     * 要素を追加する
     */
    fun add(element: T): Boolean =
        if (!_value.contains(element)) {
            _value.add(element)
            true
        } else {
            false
        }

    /**
     * 要素を削除する
     */
    fun remove(element: T): Boolean = _value.remove(element)

    /**
     * 特定の要素が含まれているか確認
     */
    fun contains(element: T): Boolean = _value.contains(element)
}
