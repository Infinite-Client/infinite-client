package org.infinite.libs.core.features

open class Property<T>(
    val default: T,
) {
    open var value: T = default

    fun reset() {
        value = default
    }
}
