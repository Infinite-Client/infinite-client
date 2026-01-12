package org.infinite.infinite.features.local.rendering.clearsight

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty

class ClearSightFeature : LocalFeature() {
    override val featureType = FeatureType.Extend

    val antiFog by property(BooleanProperty(true)) // 通常の霧・遠くの霞
    val antiOverlay by property(BooleanProperty(true)) // 水・溶岩・粉雪などの視界遮蔽
}
