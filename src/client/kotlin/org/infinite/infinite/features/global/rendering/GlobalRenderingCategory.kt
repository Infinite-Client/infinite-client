package org.infinite.infinite.features.global.rendering

import org.infinite.infinite.features.global.rendering.font.InfiniteFontFeature
import org.infinite.infinite.features.global.rendering.loading.InfiniteLoadingFeature
import org.infinite.infinite.features.global.rendering.theme.ThemeFeature
import org.infinite.infinite.features.global.rendering.uistyle.UiStyleFeature // UiStyle... を採用
import org.infinite.libs.core.features.categories.category.GlobalCategory

@Suppress("Unused")
class GlobalRenderingCategory : GlobalCategory() {
    val infiniteFontFeature by feature(InfiniteFontFeature())
    val infiniteLoadingFeature by feature(InfiniteLoadingFeature())
    val themeFeature by feature(ThemeFeature())
    val uiStyleFeature by feature(UiStyleFeature())
}
