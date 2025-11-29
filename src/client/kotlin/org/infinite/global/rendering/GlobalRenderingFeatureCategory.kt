package org.infinite.global.rendering

import org.infinite.global.GlobalFeature
import org.infinite.global.GlobalFeatureCategory
import org.infinite.global.rendering.font.FontSetting
import org.infinite.global.rendering.loading.LoadingAnimationSetting
import org.infinite.global.rendering.theme.ThemeSetting
import org.infinite.global.rendering.title.TitleScreenSetting

class GlobalRenderingFeatureCategory :
    GlobalFeatureCategory(
        "Rendering",
        mutableListOf(
            GlobalFeature(ThemeSetting()),
            GlobalFeature(FontSetting()),
            GlobalFeature(LoadingAnimationSetting()),
            GlobalFeature(TitleScreenSetting()),
        ),
    )
