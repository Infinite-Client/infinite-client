package org.infinite.global.rendering.theme

import net.minecraft.client.gui.components.AbstractWidget
import org.infinite.InfiniteClient
import org.infinite.global.ConfigurableGlobalFeature
import org.infinite.libs.gui.widget.ThemeTileButton
import org.infinite.libs.gui.widget.ThemeTileContainer
import org.infinite.settings.FeatureSetting

class ThemeSetting : ConfigurableGlobalFeature() {
    val themeSetting = FeatureSetting.StringListSetting("Theme", "infinite", mutableListOf())
    override val settings: List<FeatureSetting<*>> = listOf(themeSetting)

    override fun onTick() {
        refreshOptions()
    }

    override fun onInit() {
        refreshOptions()
        syncOptions()
    }

    fun syncOptions() {
        refreshOptions()
        InfiniteClient.currentTheme = themeSetting.value
    }

    private fun refreshOptions() {
        val names = InfiniteClient.themes.map { it.name }
        if (names.isEmpty()) return
        if (themeSetting.options != names) {
            themeSetting.options.clear()
            themeSetting.options.addAll(names)
        }
        if (!themeSetting.options.contains(themeSetting.value)) {
            themeSetting.value = themeSetting.options.first()
        }
        InfiniteClient.currentTheme = themeSetting.value
    }

    override fun getCustomWidgets(): List<AbstractWidget> {
        val buttons =
            InfiniteClient.themes.map { theme ->
                ThemeTileButton(
                    0,
                    0,
                    260,
                    36,
                    theme,
                    { themeSetting.value == theme.name },
                ) {
                    themeSetting.value = theme.name
                    InfiniteClient.currentTheme = theme.name
                }
            }
        return listOf(ThemeTileContainer(0, 0, 0, 0, buttons))
    }
}
