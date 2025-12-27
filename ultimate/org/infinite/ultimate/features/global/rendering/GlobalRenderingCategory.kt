package org.infinite.ultimate.features.global.rendering

import org.infinite.libs.core.features.categories.category.GlobalCategory
import org.infinite.ultimate.features.global.rendering.font.UltimateFontFeature
import org.infinite.ultimate.features.global.rendering.theme.ThemeFeature

class GlobalRenderingCategory : GlobalCategory() {
    val ultimateFontFeature by feature(UltimateFontFeature())
    val themeFeature by feature(ThemeFeature())
}
