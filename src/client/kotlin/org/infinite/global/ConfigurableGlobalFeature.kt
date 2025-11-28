package org.infinite.global
import net.minecraft.client.gui.widget.ClickableWidget
import org.infinite.feature.ConfigurableFeature

abstract class ConfigurableGlobalFeature : ConfigurableFeature(true) {
    open fun onInit() {}

    open fun onShutdown() {}

    open fun getCustomWidgets(): List<ClickableWidget> = emptyList()
}
