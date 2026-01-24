package org.infinite.utils

import net.minecraft.client.gui.components.AbstractWidget
import org.infinite.InfiniteClient.theme
import org.infinite.libs.graphics.bundle.Graphics2DRenderer

object WidgetRenderUtils {

    fun renderCustomBackground(widget: AbstractWidget, renderer: Graphics2DRenderer) {
        val theme = theme
        val colorScheme = theme.colorScheme

        val alpha = widget.getAlpha()
        val alphaInt = (alpha * 255).toInt()

        val bgAlpha = if (widget.active) alpha else alpha / 2f
        val radius = (widget.height * 0.35f).coerceAtLeast(6f)
        val baseColor = colorScheme.surfaceColor.mix(colorScheme.backgroundColor, 0.5f)
        renderer.fillStyle = baseColor.alpha((255 * bgAlpha).toInt())
        renderer.fillRoundedRect(
            widget.x.toFloat(),
            widget.y.toFloat(),
            widget.width.toFloat(),
            widget.height.toFloat(),
            radius,
        )
    }
}
