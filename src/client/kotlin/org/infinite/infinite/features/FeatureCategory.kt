package org.infinite.infinite.features

import org.infinite.libs.feature.ConfigurableFeature

open class FeatureCategory(
    val name: String,
    open val features: MutableList<Feature<out ConfigurableFeature>>,
)
