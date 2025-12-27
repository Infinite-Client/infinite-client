package org.infinite.libs.global.rendering.font

import net.minecraft.resources.Identifier
import org.infinite.infinite.features.rendering.font.HyperTextRenderer
import org.infinite.libs.global.ConfigurableGlobalFeature
import org.infinite.settings.FeatureSetting

class FontSetting : ConfigurableGlobalFeature() {
    override val settings: List<FeatureSetting<*>> = listOf()
    private val hyperTextRenderer: HyperTextRenderer?
        get() = client.font as? HyperTextRenderer

    override fun onEnabled() {
        hyperTextRenderer?.enable()
    }

    override fun onInit() {
        hyperTextRenderer?.defineFont(
            HyperTextRenderer.HyperFonts(
                Identifier.fromNamespaceAndPath("minecraft", "infinite_regular"),
                Identifier.fromNamespaceAndPath("minecraft", "infinite_italic"),
                Identifier.fromNamespaceAndPath("minecraft", "infinite_bold"),
                Identifier.fromNamespaceAndPath("minecraft", "infinite_bolditalic"),
            ),
        )
        if (isEnabled()) {
            onEnabled()
        } else {
            onDisabled()
        }
    }

    override fun onDisabled() {
        hyperTextRenderer?.disable()
    }
}
