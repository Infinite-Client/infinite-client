package org.infinite.libs.core.features

import org.infinite.libs.ui.widgets.PropertyWidget
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

open class Property<T : Any>(
    val default: T,
) {
    // 追加: 自身の名称と親Featureの参照
    var name: String = "unknown"
        internal set
    var parent: Feature? = null
        internal set
    private val valueType = default::class.java
    private val _value = AtomicReference<T>(default)
    private val listeners = CopyOnWriteArrayList<(oldValue: T, newValue: T) -> Unit>()

    /**
     * プロパティの翻訳キーを生成する。
     * 例: infinite.features.cheat.movement.flight.speed
     */
    fun translationKey(): String? {
        val parentKey = parent?.translation() ?: return null
        return "$parentKey.$name"
    }

    var value: T
        get() = _value.get()
        set(newValue) {
            val filtered = filterValue(newValue)
            val oldValue = _value.getAndSet(filtered)
            if (oldValue != filtered) {
                notifyListeners(oldValue, filtered)
            }
        }

    protected open fun filterValue(newValue: T): T = newValue
    fun reset() {
        value = default
    }

    fun addListener(listener: (oldValue: T, newValue: T) -> Unit) = listeners.add(listener)
    fun removeListener(listener: (oldValue: T, newValue: T) -> Unit) = listeners.remove(listener)
    protected fun notifyListeners(oldValue: T, newValue: T) {
        listeners.forEach { it(oldValue, newValue) }
    }

    fun tryApply(anyValue: Any?) {
        if (anyValue == null) return

        // 1. 型が一致していれば即代入
        if (valueType.isInstance(anyValue)) {
            @Suppress("UNCHECKED_CAST")
            this.value = anyValue as T
            return
        }

        // 2. 数値型・論理型の変換を効率化
        val converted: Any? = when (valueType) {
            Int::class.javaObjectType, Int::class.java -> (anyValue as? Number)?.toInt()
            Long::class.javaObjectType, Long::class.java -> (anyValue as? Number)?.toLong()
            Float::class.javaObjectType, Float::class.java -> (anyValue as? Number)?.toFloat()
            Double::class.javaObjectType, Double::class.java -> (anyValue as? Number)?.toDouble()
            Boolean::class.javaObjectType, Boolean::class.java -> {
                when (anyValue) {
                    is Boolean -> anyValue
                    is Number -> anyValue.toLong() != 0L
                    is String -> anyValue.toBoolean()
                    else -> null
                }
            }

            String::class.java -> anyValue.toString()
            else -> null
        }

        if (converted != null) {
            @Suppress("UNCHECKED_CAST")
            this.value = converted as T
        }
    }

    open fun widget(x: Int, y: Int, width: Int): PropertyWidget<*> =
        PropertyWidget(x, y, width, property = this)
}
