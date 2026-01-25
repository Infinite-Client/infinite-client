package org.infinite.infinite.features.global.rendering.theme

import net.minecraft.client.gui.screens.ChatScreen
import org.infinite.InfiniteClient
import org.infinite.infinite.features.global.rendering.theme.renderer.EditBoxRenderer
import org.infinite.infinite.features.global.rendering.theme.renderer.PlainButtonRenderer
import org.infinite.infinite.features.global.rendering.theme.renderer.ScrollWidgetRenderer
import org.infinite.infinite.features.global.rendering.theme.renderer.SliderButtonRenderer
import org.infinite.libs.core.features.feature.GlobalFeature
import org.infinite.libs.core.features.property.SelectionProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.ui.style.UiStyle

class ThemeFeature : GlobalFeature() {
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

    val currentTheme by property(ThemeSelectionProperty())
    val style by property(EnumSelectionProperty(UiStyle.Simple))
}
