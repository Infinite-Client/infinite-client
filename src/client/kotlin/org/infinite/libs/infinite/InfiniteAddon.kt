package org.infinite.libs.infinite

import org.infinite.infinite.features.FeatureCategory
import org.infinite.libs.global.GlobalFeatureCategory
import org.infinite.libs.gui.theme.Theme

interface InfiniteAddon {
    val id: String
    val version: String

    val features: List<FeatureCategory>
    val globalFeatures: List<GlobalFeatureCategory>
    val themes: List<Theme>

    fun onInit()

    fun onShutdown()
}
