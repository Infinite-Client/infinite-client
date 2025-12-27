package org.infinite.ultimate

import org.infinite.libs.core.features.categories.GlobalFeatureCategories
import org.infinite.ultimate.features.global.rendering.GlobalRenderingCategory

class UltimateGlobalFeatures : GlobalFeatureCategories() {
    val rendering by category(GlobalRenderingCategory())
}
