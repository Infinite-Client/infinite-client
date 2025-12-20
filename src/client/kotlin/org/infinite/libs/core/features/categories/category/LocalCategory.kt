package org.infinite.libs.core.features.categories.category

import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.feature.LocalFeature
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class LocalCategory : Category<KClass<out LocalFeature>, LocalFeature>() {
    override val features: ConcurrentHashMap<KClass<out LocalFeature>, LocalFeature> = ConcurrentHashMap()
}
