package org.infinite.libs.ui.theme

import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha

abstract class Theme {
    open val colorScheme = ColorScheme()
    open fun renderBackGround(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        graphics2DRenderer: Graphics2DRenderer,
        alpha: Float = 1.0f,
    ) {
        val backgroundColor = colorScheme.backgroundColor
        graphics2DRenderer.fillStyle = backgroundColor.alpha((255 * alpha).toInt())
        graphics2DRenderer.fillRect(x, y, width, height)
        graphics2DRenderer.flush()
    }
}
