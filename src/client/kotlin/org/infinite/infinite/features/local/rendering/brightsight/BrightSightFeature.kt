package org.infinite.infinite.features.local.rendering.brightsight

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty

class BrightSightFeature : LocalFeature() {
    override val featureType = FeatureType.Extend

    enum class Method {
        GamMax,
        NightSight,
        UltraBright,
    }
    val method by property(EnumSelectionProperty(Method.GamMax))
}
