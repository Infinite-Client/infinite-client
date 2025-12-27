package org.infinite.libs.global
import net.minecraft.client.gui.components.AbstractWidget
import org.infinite.libs.feature.ConfigurableFeature

abstract class ConfigurableGlobalFeature : ConfigurableFeature(true) {
    open fun onInit() {}

    open fun onShutdown() {}

    open fun getCustomWidgets(): List<AbstractWidget> = emptyList()
}
