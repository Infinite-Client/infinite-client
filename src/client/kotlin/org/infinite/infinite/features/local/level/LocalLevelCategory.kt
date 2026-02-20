package org.infinite.infinite.features.local.level

import org.infinite.infinite.features.local.level.blockbreak.FastBreakFeature
import org.infinite.infinite.features.local.level.blockbreak.LinearBreakFeature
import org.infinite.infinite.features.local.level.blockbreak.VeinBreakFeature
import org.infinite.infinite.features.local.level.esp.EspFeature
import org.infinite.infinite.features.local.level.highlight.BlockHighlightFeature
import org.infinite.infinite.features.local.level.highlight.CaveHighlightFeature
import org.infinite.infinite.features.local.level.tag.UltraTagFeature
import org.infinite.infinite.features.local.level.xray.XRayFeature
import org.infinite.libs.core.features.categories.category.LocalCategory

@Suppress("Unused")
class LocalLevelCategory : LocalCategory() {
    val xRayFeature by feature(XRayFeature())
    val espFeature by feature(EspFeature())
    val ultraTagFeature by feature(UltraTagFeature())
    val blockHighlightFeature by feature(BlockHighlightFeature())
    val caveHighlightFeature by feature(CaveHighlightFeature())
    val linearBreakFeature by feature(LinearBreakFeature())
    val veinBreakFeature by feature(VeinBreakFeature())
    val fastBreakFeature by feature(FastBreakFeature())
}
