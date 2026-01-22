package org.infinite.infinite.features.local.rendering.ui

import org.infinite.infinite.features.local.rendering.ui.crosshair.CrosshairRenderer
import org.infinite.infinite.features.local.rendering.ui.hotbar.HotbarRenderer
import org.infinite.infinite.features.local.rendering.ui.left.LeftBoxRenderer
import org.infinite.infinite.features.local.rendering.ui.right.RightBoxRenderer
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.graphics.Graphics2D

class UltraUiFeature : LocalFeature() {
    val crosshairRenderer = CrosshairRenderer()
    val hotbarRenderer = HotbarRenderer()
    val leftBoxRenderer = LeftBoxRenderer()
    val rightBoxRenderer = RightBoxRenderer()

    val hotbarUi by property(BooleanProperty(true))
    val leftBoxUi by property(BooleanProperty(true))
    val rightBoxUi by property(BooleanProperty(true))
    val crosshairUi by property(BooleanProperty(true))
    override fun onStartUiRendering(graphics2D: Graphics2D) {
        if (hotbarUi.value) {
            hotbarRenderer.render(graphics2D)
        }
        if (leftBoxUi.value) {
            leftBoxRenderer.render(graphics2D)
        }
        if (rightBoxUi.value) {
            rightBoxRenderer.render(graphics2D)
        }
        if (crosshairUi.value) {
            crosshairRenderer.render(graphics2D)
        }
    }
}
