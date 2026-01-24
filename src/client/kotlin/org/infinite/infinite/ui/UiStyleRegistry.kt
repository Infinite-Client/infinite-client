package org.infinite.infinite.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.infinite.infinite.ui.screen.FeatureScreen
import org.infinite.infinite.ui.screen.GlobalFeatureCategoriesScreen
import org.infinite.infinite.ui.screen.LocalFeatureCategoriesScreen
import org.infinite.libs.core.features.Feature
import java.util.concurrent.ConcurrentHashMap

interface UiStyleProvider {
    fun openLocalFeatures(parent: Screen? = null)
    fun openGlobalFeatures(parent: Screen? = null)
    fun openFeatureSettings(feature: Feature, parent: Screen)
}

class ClickGuiStyleProvider : UiStyleProvider {
    override fun openLocalFeatures(parent: Screen?) {
        val mc = Minecraft.getInstance()
        mc.execute { mc.setScreen(LocalFeatureCategoriesScreen(parent)) }
    }

    override fun openGlobalFeatures(parent: Screen?) {
        val mc = Minecraft.getInstance()
        mc.execute { mc.setScreen(GlobalFeatureCategoriesScreen(parent)) }
    }

    override fun openFeatureSettings(feature: Feature, parent: Screen) {
        val mc = Minecraft.getInstance()
        mc.execute { mc.setScreen(FeatureScreen(feature, parent)) }
    }
}

object UiStyleRegistry {
    const val DEFAULT_STYLE = "Click GUI"

    private val providers = ConcurrentHashMap<String, UiStyleProvider>()

    init {
        register(DEFAULT_STYLE, ClickGuiStyleProvider())
    }

    fun register(name: String, provider: UiStyleProvider) {
        providers[name] = provider
    }

    fun availableStyles(): List<String> = providers.keys.sorted()

    fun provider(name: String?): UiStyleProvider = providers[name] ?: providers[DEFAULT_STYLE] ?: ClickGuiStyleProvider()
}
