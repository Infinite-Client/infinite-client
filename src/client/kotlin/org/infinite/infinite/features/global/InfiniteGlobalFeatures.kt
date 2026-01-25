package org.infinite.infinite.features.global

import org.infinite.infinite.features.global.control.GlobalControlCategory
import org.infinite.infinite.features.global.rendering.GlobalRenderingCategory
import org.infinite.libs.core.features.categories.GlobalFeatureCategories

@Suppress("Unused")
class InfiniteGlobalFeatures : GlobalFeatureCategories() {
    val rendering by category(GlobalRenderingCategory())
    val control by category(GlobalControlCategory())
}
