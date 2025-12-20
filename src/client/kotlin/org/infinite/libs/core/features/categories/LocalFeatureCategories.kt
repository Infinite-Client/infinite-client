package org.infinite.libs.core.features.categories

import org.infinite.libs.core.features.FeatureCategories
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.core.features.feature.LocalFeature
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class LocalFeatureCategories : FeatureCategories<KClass<out LocalFeature>, LocalFeature, KClass<out LocalCategory>, LocalCategory>() {
    override val categories: ConcurrentHashMap<KClass<out LocalCategory>, LocalCategory> = ConcurrentHashMap()
}
