package org.infinite.infinite.features.local.level

import org.infinite.infinite.features.local.level.xray.XRayFeature
import org.infinite.libs.core.features.categories.category.LocalCategory

@Suppress("Unused")
class LocalLevelCategory : LocalCategory() {
    val xRayFeature by feature(XRayFeature())
}
