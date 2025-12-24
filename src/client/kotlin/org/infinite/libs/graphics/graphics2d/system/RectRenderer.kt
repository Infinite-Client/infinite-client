package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fill

class RectRenderer(
    private val guiGraphics: GuiGraphics,
) {
    /**
     * 矩形の枠線を描画する
     */
    fun strokeRect(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
        strokeWidth: Float,
    ) {
        // 上辺
        guiGraphics.fill(x1, y1, x2, y1 + strokeWidth, color)
        // 下辺
        guiGraphics.fill(x1, y2 - strokeWidth, x2, y2, color)
        // 左辺
        guiGraphics.fill(x1, y1 + strokeWidth, x1 + strokeWidth, y2 - strokeWidth, color)
        // 右辺
        guiGraphics.fill(x2 - strokeWidth, y1 + strokeWidth, x2, y2 - strokeWidth, color)
    }

    fun strokeRect(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        color: Int,
        strokeWidth: Int,
    ) = strokeRect(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), color, strokeWidth.toFloat())

    fun strokeRect(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        color: Int,
        strokeWidth: Double,
    ) = strokeRect(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), color, strokeWidth.toFloat())

    /**
     * 矩形を塗りつぶす
     */
    fun fillRect(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
    ) = guiGraphics.fill(x1, y1, x2, y2, color)

    fun fillRect(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        color: Int,
    ) = guiGraphics.fill(x1, y1, x2, y2, color)

    fun fillRect(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        color: Int,
    ) = guiGraphics.fill(x1, y1, x2, y2, color)
}
