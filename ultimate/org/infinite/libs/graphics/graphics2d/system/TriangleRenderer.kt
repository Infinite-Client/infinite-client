package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fillTriangle

class TriangleRenderer(
    private val guiGraphics: GuiGraphics,
) {
    /**
     * 三角形を塗りつぶす（各頂点の色を指定可能）
     */
    fun fillTriangle(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        col0: Int,
        col1: Int,
        col2: Int,
    ) {
        guiGraphics.fillTriangle(x0, y0, x1, y1, x2, y2, col0, col1, col2)
    }

    /**
     * 単一色で三角形を塗りつぶす
     */
    fun fillTriangle(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
    ) = fillTriangle(x0, y0, x1, y1, x2, y2, color, color, color)
}
