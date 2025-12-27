package org.infinite.ultimate

import org.infinite.libs.core.features.categories.LocalFeatureCategories
import org.infinite.ultimate.features.local.rendering.LocalRenderingCategory

class UltimateLocalFeatures : LocalFeatureCategories() {
    val rendering by category(LocalRenderingCategory())
}
