package org.infinite.infinite.features.local.rendering.stablesight

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.FloatProperty

class StableSightFeature : LocalFeature() {
    val cameraDistance by property(FloatProperty(10f, -10f, 64f))
    val ignoreTerrain by property(BooleanProperty(true))
}
