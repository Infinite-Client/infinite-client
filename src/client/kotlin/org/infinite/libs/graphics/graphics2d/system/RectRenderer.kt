package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fill
import org.infinite.libs.graphics.graphics2d.minecraft.fillQuad

class RectRenderer(
    private val guiGraphics: GuiGraphics,
) {
    // --- Fill Logic ---

    fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        guiGraphics.fill(x, y, x + w, y + h, color)
    }

    fun fillRect(x: Float, y: Float, w: Float, h: Float, col0: Int, col1: Int, col2: Int, col3: Int) {
        guiGraphics.fillQuad(
            x, y, // 左上 (0)
            x + w, y, // 右上 (1)
            x + w, y + h, // 右下 (2)
            x, y + h, // 左下 (3)
            col0, col1, col2, col3,
        )
    }

    // --- Stroke Logic ---

    fun strokeRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
        strokeWidth: Float,
    ) {
        strokeRect(x, y, w, h, color, color, color, color, strokeWidth)
    }

    /**
     * 四隅の色を指定する枠線描画
     */
    fun strokeRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        col0: Int, // 左上
        col1: Int, // 右上
        col2: Int, // 右下
        col3: Int, // 左下
        strokeWidth: Float,
    ) {
        val v = strokeWidth / 2f
        val p1 = PointPair(x + v, y + v, x - v, y - v)
        val p2 = PointPair(x + v, y + h - v, x - v, y + h + v)
        val p3 = PointPair(x + w - v, y + h - v, x + w + v, y + h + v)
        val p4 = PointPair(x + w - v, y + v, x + w + v, y - v)
        guiGraphics.fillQuad(p1.ix, p1.iy, p1.ox, p1.oy, p2.ox, p2.oy, p2.ix, p2.iy, col0, col0, col1, col1)
        guiGraphics.fillQuad(p2.ix, p2.iy, p2.ox, p2.oy, p3.ox, p3.oy, p3.ix, p3.iy, col1, col1, col2, col2)
        guiGraphics.fillQuad(p3.ix, p3.iy, p3.ox, p3.oy, p4.ox, p4.oy, p4.ix, p4.iy, col2, col2, col3, col3)
        guiGraphics.fillQuad(p4.ix, p4.iy, p4.ox, p4.oy, p1.ox, p1.oy, p1.ix, p1.iy, col3, col3, col0, col0)
    }
}
