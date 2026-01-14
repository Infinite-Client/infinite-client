package org.infinite.libs.ui.theme

import org.infinite.libs.graphics.Graphics2D
import org.infinite.utils.alpha

abstract class Theme {
    open val colorScheme = ColorScheme()
    open fun renderBackGround(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        graphics2D: Graphics2D,
        alpha: Float = 1.0f,
    ) {
        val backgroundColor = colorScheme.backgroundColor
        graphics2D.fillStyle = backgroundColor.alpha((255 * alpha).toInt())
        graphics2D.fillRect(x, y, width, height)
    }
    fun renderBackGround(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        graphics2D: Graphics2D,
        alpha: Float = 1.0f,
    ) = renderBackGround(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), graphics2D, alpha)
}
