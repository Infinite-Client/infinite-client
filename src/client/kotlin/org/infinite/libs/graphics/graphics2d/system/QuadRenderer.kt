package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fillQuad

class QuadRenderer(
    private val guiGraphics: GuiGraphics,
) {
    /**
     * 四角形を塗りつぶす（各頂点の色を指定可能）
     */
    fun fillQuad(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        col0: Int, col1: Int, col2: Int, col3: Int
    ) {
        guiGraphics.fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, col0, col1, col2, col3)
    }

    /**
     * 単一色で四角形を塗りつぶす（Float）
     */
    fun fillQuad(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        color: Int
    ) = fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, color, color, color, color)

    /**
     * 単一色で四角形を塗りつぶす（Double）
     */
    fun fillQuad(
        x0: Double, y0: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double,
        x3: Double, y3: Double,
        color: Int
    ) = fillQuad(
        x0.toFloat(), y0.toFloat(),
        x1.toFloat(), y1.toFloat(),
        x2.toFloat(), y2.toFloat(),
        x3.toFloat(), y3.toFloat(),
        color
    )
}
