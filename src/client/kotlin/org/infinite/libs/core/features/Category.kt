package org.infinite.libs.core.features

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

abstract class Category<K : KClass<out Feature>, V : Feature> {
    abstract val features: ConcurrentHashMap<K, V>
}
