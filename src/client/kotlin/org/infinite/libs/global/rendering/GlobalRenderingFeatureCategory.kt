package org.infinite.libs.global.rendering

import org.infinite.libs.global.GlobalFeature
import org.infinite.libs.global.GlobalFeatureCategory
import org.infinite.libs.global.rendering.font.FontSetting
import org.infinite.libs.global.rendering.loading.LoadingAnimationSetting
import org.infinite.libs.global.rendering.theme.ThemeSetting

class GlobalRenderingFeatureCategory :
    GlobalFeatureCategory(
        "Rendering",
        mutableListOf(
            GlobalFeature(ThemeSetting()),
            GlobalFeature(FontSetting()),
            GlobalFeature(LoadingAnimationSetting()),
        ),
    )
