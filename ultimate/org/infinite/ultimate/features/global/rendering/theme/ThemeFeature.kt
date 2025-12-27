package org.infinite.ultimate.features.global.rendering.theme

import org.infinite.UltimateClient
import org.infinite.libs.core.features.feature.GlobalFeature
import org.infinite.libs.core.features.property.SelectionProperty

class ThemeFeature : GlobalFeature() {
    /**
     * ThemeManager から動的に選択肢を取得する専用プロパティ
     */
    class ThemeSelectionProperty : SelectionProperty<String>(
        "DefaultTheme",
        emptyList(),
    ) {
        override val options: List<String>
            get() = UltimateClient.themeManager.getRegisteredThemeNames()
    }

    // デリゲートプロパティとして登録
    val currentTheme by property(ThemeSelectionProperty())
}
