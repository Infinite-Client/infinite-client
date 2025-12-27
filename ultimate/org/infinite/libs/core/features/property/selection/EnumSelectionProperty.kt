package org.infinite.libs.core.features.property.selection

import org.infinite.libs.core.features.property.SelectionProperty

/**
 * Enumの全要素を選択肢として保持するプロパティ
 *
 * @param T Enumの型
 * @param default デフォルト値
 */
open class EnumSelectionProperty<T : Enum<T>>(
    default: T,
) : SelectionProperty<T>(
    default = default,
    opts = default.declaringJavaClass.enumConstants.toList(),
) {
    companion object {
        /**
         * 型引数から自動的に全要素を取得して生成するファクトリメソッド
         */
        inline fun <reified T : Enum<T>> create(default: T): EnumSelectionProperty<T> {
            return EnumSelectionProperty(default)
        }
    }
}
