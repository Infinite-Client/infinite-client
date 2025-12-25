package org.infinite.libs.core.features.property

import org.infinite.libs.core.features.Property

/**
 * 数値系設定の基底クラス
 * @param T 数値型 (Int, Double, Float 等)
 * @param default デフォルト値
 * @param min 最小値
 * @param max 最大値
 * @param suffix 単位などの接尾語 (例: "fps", "%", " blocks")
 */
open class NumberProperty<T>(
    default: T,
    val min: T,
    val max: T,
    val suffix: String = "",
) : Property<T>(default) where T : Number, T : Comparable<T> {
    // Propertyクラスのvalueをオーバーライドして、範囲制限ロジックを追加
    override var value: T = default
        set(newValue) {
            field =
                when {
                    newValue < min -> min
                    newValue > max -> max
                    else -> newValue
                }
        }

    fun display(): String = "$value$suffix"
}
