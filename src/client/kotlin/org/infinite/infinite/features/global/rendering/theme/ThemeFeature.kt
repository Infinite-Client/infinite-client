package org.infinite.infinite.features.global.rendering.theme

import net.minecraft.client.gui.screens.ChatScreen
import org.infinite.InfiniteClient
import org.infinite.infinite.features.global.rendering.theme.renderer.EditBoxRenderer
import org.infinite.infinite.features.global.rendering.theme.renderer.PlainButtonRenderer
import org.infinite.infinite.features.global.rendering.theme.renderer.ScrollWidgetRenderer
import org.infinite.infinite.features.global.rendering.theme.renderer.SliderButtonRenderer
import org.infinite.libs.core.features.feature.GlobalFeature
import org.infinite.libs.core.features.property.SelectionProperty

class ThemeFeature : GlobalFeature() {
    init {
        enabled.value = true
    }

    val sliderButtonRenderer = SliderButtonRenderer()
    val scrollWidgetRenderer = ScrollWidgetRenderer()
    val plainButtonRenderer = PlainButtonRenderer()
    val editBoxRenderer = EditBoxRenderer()

    /**
     * ThemeManager から動的に選択肢を取得する専用プロパティ
     */
    class ThemeSelectionProperty :
        SelectionProperty<String>(
            "DefaultTheme",
            emptyList(),
        ) {
        override val options: List<String>
            get() = InfiniteClient.themeManager.getRegisteredThemeNames()
    }
    fun shouldRenderCustom(): Boolean = isEnabled() && minecraft.screen !is ChatScreen

    // デリゲートプロパティとして登録
    val currentTheme by property(ThemeSelectionProperty())
}
