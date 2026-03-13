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
    // Enumのクラス型を保持しておく（逆引き用）
    private val enumClass: Class<T> = default.declaringJavaClass

    override fun tryApply(anyValue: Any?) {
        if (anyValue == null) return

        // 1. すでに同じEnum型の場合
        if (enumClass.isInstance(anyValue)) {
            @Suppress("UNCHECKED_CAST")
            this.value = anyValue as T
            return
        }

        // それ以外（JsonPrimitive, String, Numberなど）は親クラスのロジックに任せる
        super.tryApply(anyValue)
    }
}
