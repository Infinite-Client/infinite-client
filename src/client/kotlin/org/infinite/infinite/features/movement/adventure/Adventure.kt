package org.infinite.infinite.features.movement.adventure

import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting

class Adventure : ConfigurableFeature(initialEnabled = false) {
    override val level: FeatureLevel = FeatureLevel.Extend
    override val settings: List<FeatureSetting<*>> = emptyList()
}
