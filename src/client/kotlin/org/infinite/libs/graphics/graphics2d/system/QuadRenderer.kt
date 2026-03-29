package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.infinite.libs.graphics.graphics2d.minecraft.fillQuad
import org.infinite.libs.graphics.graphics2d.normalizeToCCW

class QuadRenderer(private val guiGraphics: GuiGraphicsExtractor) {

    fun fillQuad(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        col0: Int,
        col1: Int,
        col2: Int,
        col3: Int,
    ) {
        val q = normalizeToCCW(x0, y0, x1, y1, x2, y2, x3, y3, col0, col1, col2, col3)
        guiGraphics.fillQuad(q.x0, q.y0, q.x1, q.y1, q.x2, q.y2, q.x3, q.y3, q.c0, q.c1, q.c2, q.c3)
    }

    fun fillQuad(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        color: Int,
    ) {
        fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, color, color, color, color)
    }
}
