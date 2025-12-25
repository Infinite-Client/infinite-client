package org.infinite.libs.core.features

import org.infinite.libs.interfaces.MinecraftInterface
import java.util.concurrent.ConcurrentHashMap

open class Feature : MinecraftInterface() {
    open val properties: ConcurrentHashMap<String, Property<*>> = ConcurrentHashMap()
    private val translationParent =
        init {
            val kClass = this::class
            val qualifiedName: String? = kClass.qualifiedName
        }
}
