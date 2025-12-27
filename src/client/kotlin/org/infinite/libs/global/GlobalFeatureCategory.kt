package org.infinite.libs.global

open class GlobalFeatureCategory(
    val name: String,
    val features: MutableList<GlobalFeature<out ConfigurableGlobalFeature>>,
)
