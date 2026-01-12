package org.infinite.infinite.features.local.rendering.clearsight

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.FloatProperty

class ClearSightFeature : LocalFeature() {
    override val featureType = FeatureType.Extend

    val antiFog by property(BooleanProperty(true)) // 通常の霧・遠くの霞
    val antiOverlay by property(BooleanProperty(true)) // 水・溶岩・粉雪などの視界遮蔽
    val antiFovChange by property(BooleanProperty(true))

    // FOV抑制のパラメータ
    // maxIncrease: 0.0 (変化なし) ～ 0.5 (大幅な広がりを許容)
    val fovMaxIncrease by property(FloatProperty(0.12f, 0.0f, 0.5f))

    // intensity: 1.0 (緩やか) ～ 20.0 (即座にクランプ)
    val fovIntensity by property(FloatProperty(8.0f, 1.0f, 20.0f))
}
