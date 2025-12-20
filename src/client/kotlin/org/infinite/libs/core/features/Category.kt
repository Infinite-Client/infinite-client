package org.infinite.libs.core.features

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

abstract class Category<K : KClass<out Feature>, V : Feature> {
    abstract val features: ConcurrentHashMap<K, V>

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : V> insert(feature: T) {
        features[T::class as K] = feature
    }
}
