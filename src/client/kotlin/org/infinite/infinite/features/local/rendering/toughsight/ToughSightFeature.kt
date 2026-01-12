package org.infinite.infinite.features.local.rendering.toughsight

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty

class ToughSightFeature : LocalFeature() {
    override val featureType = FeatureType.Extend

    val antiNausea by property(BooleanProperty(true)) // 吐き気
    val antiBlindness by property(BooleanProperty(true)) // 盲目
    val antiDarkness by property(BooleanProperty(true)) // 暗闇
}
