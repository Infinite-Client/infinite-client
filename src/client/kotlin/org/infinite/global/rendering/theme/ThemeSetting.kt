package org.infinite.global.rendering.theme

import org.infinite.InfiniteClient
import org.infinite.global.ConfigurableGlobalFeature
import org.infinite.settings.FeatureSetting

class ThemeSetting : ConfigurableGlobalFeature() {
    val themeSetting = FeatureSetting.StringListSetting("Theme", "infinite", mutableListOf())
    override val settings: List<FeatureSetting<*>> = listOf(themeSetting)

    override fun onTick() {
        refreshOptions()
        InfiniteClient.currentTheme = themeSetting.value
    }

    override fun onInit() {
        refreshOptions()
    }

    fun syncOptions() = refreshOptions()

    private fun refreshOptions() {
        val names = InfiniteClient.themes.map { it.name }
        if (names.isEmpty()) return

        // Update options only when they change to avoid churn.
        if (themeSetting.options != names) {
            themeSetting.options.clear()
            themeSetting.options.addAll(names)
        }

        // Ensure current value is valid; fall back to first option.
        if (!themeSetting.options.contains(themeSetting.value)) {
            themeSetting.value = themeSetting.options.first()
        }
    }
}
