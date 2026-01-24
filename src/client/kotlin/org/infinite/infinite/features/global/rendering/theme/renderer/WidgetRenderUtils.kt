package org.infinite.infinite.features.global.rendering.theme.renderer

import net.minecraft.client.gui.components.AbstractWidget
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
import org.infinite.utils.mix

object WidgetRenderUtils {

    fun renderCustomBackground(widget: AbstractWidget, renderer: Graphics2DRenderer) {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme

        val alpha = widget.getAlpha()

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
        renderer.strokeStyle.color = when {
            widget.isFocused -> colorScheme.accentColor
            widget.isHovered -> colorScheme.accentColor.mix(colorScheme.foregroundColor, 0.5f)
            !widget.isActive -> colorScheme.backgroundColor
            else -> colorScheme.secondaryColor
        }
        renderer.strokeStyle.width = 1f
        renderer.strokeRoundedRect(
            widget.x.toFloat(),
            widget.y.toFloat(),
            widget.width.toFloat(),
            widget.height.toFloat(),
            radius,
        )
    }
}
