package org.infinite.libs.core.features.property

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.infinite.libs.core.features.Property
import org.infinite.libs.ui.widgets.NumberPropertyWidget
import org.infinite.libs.ui.widgets.PropertyWidget

abstract class NumberProperty<T>(
    default: T,
    val min: T,
    val max: T,
    val suffix: String = "",
) : Property<T>(default) where T : Number, T : Comparable<T> {

    // value を書く必要がない（書けない）
    override fun filterValue(newValue: T): T = when {
        newValue < min -> min
        newValue > max -> max
        else -> newValue
    }

    override fun tryApply(anyValue: Any?) {
        if (anyValue == null) return

        // 1. 入力が数値型か文字列型かを確認
        val num: Number = when (anyValue) {
            is JsonPrimitive -> anyValue.doubleOrNull ?: anyValue.longOrNull ?: return
            is Number -> anyValue
            is String -> anyValue.toDoubleOrNull() ?: anyValue.toLongOrNull() ?: return
            else -> return
        }

        val converted: Any? = when (default) {
            is Int -> num.toInt()
            is Long -> num.toLong()
            is Float -> num.toFloat()
            is Double -> num.toDouble()
            is Byte -> num.toByte()
            is Short -> num.toShort()
            else -> null
        }

        if (converted != null) {
            @Suppress("UNCHECKED_CAST")
            this.value = converted as T
        }
    }

    open fun display(): String = "${value}$suffix"
    override fun widget(x: Int, y: Int, width: Int): PropertyWidget<NumberProperty<T>> = NumberPropertyWidget<T>(x, y, width, this)
}
