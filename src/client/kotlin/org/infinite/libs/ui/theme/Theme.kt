package org.infinite.libs.ui.theme

import org.infinite.libs.graphics.bundle.Graphics2DRenderer

abstract class Theme {
    open val colorScheme = ColorScheme()
    open fun renderBackGround(x: Float, y: Float, width: Float, height: Float, graphics2DRenderer: Graphics2DRenderer) {
        val backgroundColor = colorScheme.backgroundColor
        graphics2DRenderer.fillStyle = backgroundColor
        graphics2DRenderer.fillRect(x, y, width, height)
        graphics2DRenderer.render()
    }
}
