package org.infinite.libs.core.features

import org.infinite.libs.ui.widgets.PropertyWidget
import org.infinite.utils.toLowerSnakeCase
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

abstract class Property<T : Any>(
    val default: T,
) {
    // 追加: 自身の名称と親Featureの参照
    var name: String = "unknown"
        internal set
    var parent: Feature? = null
        internal set
    private val _value = AtomicReference<T>(default)
    private val listeners = CopyOnWriteArrayList<(oldValue: T, newValue: T) -> Unit>()

    /**
     * プロパティの翻訳キーを生成する。
     * 例: infinite.features.cheat.movement.flight.speed
     */
    fun translationKey(): String? {
        val parentKey = parent?.translation() ?: return null
        return "$parentKey.${name.toLowerSnakeCase()}"
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

    abstract fun tryApply(anyValue: Any?)

    open fun widget(x: Int, y: Int, width: Int): PropertyWidget<out Property<T>> = PropertyWidget(x, y, width, property = this)
}
