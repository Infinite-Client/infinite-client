package org.infinite.libs.core.features.property

import org.infinite.libs.core.features.Property

/**
 * 複数の選択肢から1つを選択するプロパティ
 * @param T 選択肢の型
 * @param default デフォルト値
 * @param opts 利用可能な選択肢のリスト
 */
open class SelectionProperty<T>(
    default: T,
    opts: List<T>,
) : Property<T>(default) {
    open val options = opts
    override var value: T = default
        set(newValue) {
            if (options.contains(newValue)) field = newValue
        }

    fun next() {
        val currentIndex = options.indexOf(value)
        value = options[(currentIndex + 1) % options.size]
    }
}
