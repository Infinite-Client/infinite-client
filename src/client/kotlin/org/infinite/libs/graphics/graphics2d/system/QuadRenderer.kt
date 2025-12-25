package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fillQuad
import org.infinite.libs.graphics.graphics2d.system.PointPair.Companion.calculateOffsets

class QuadRenderer(private val guiGraphics: GuiGraphics) {

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
        guiGraphics.fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, col0, col1, col2, col3)
    }

    fun strokeQuad(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        color: Int,
        strokeWidth: Float,
    ) {
        val halfWidth = strokeWidth / 2f

        // 1. 元のロジック同様、重心を計算（非常に軽量）
        val cx = (x0 + x1 + x2 + x3) / 4f
        val cy = (y0 + y1 + y2 + y3) / 4f

        // 2. PointPair (内外のオフセット) を計算（元の calculateOffsets を使用）
        val p0 = calculateOffsets(x0, y0, cx, cy, halfWidth)
        val p1 = calculateOffsets(x1, y1, cx, cy, halfWidth)
        val p2 = calculateOffsets(x2, y2, cx, cy, halfWidth)
        val p3 = calculateOffsets(x3, y3, cx, cy, halfWidth)

        // 3. 4つの辺を描画（頂点の並び順を修正し、ねじれとカリングを防止）
        // 頂点順序: [開始外, 終了外, 終了内, 開始内] の順で渡すと綺麗に繋がります
        drawEdge(p0, p1, color)
        drawEdge(p1, p2, color)
        drawEdge(p2, p3, color)
        drawEdge(p3, p0, color)
    }

    private fun drawEdge(start: PointPair, end: PointPair, color: Int) {
        // fillQuad に渡す順序を「外側2点 -> 内側2点」に固定
        guiGraphics.fillQuad(
            start.ox, start.oy,
            end.ox, end.oy,
            end.ix, end.iy,
            start.ix, start.iy,
            color, color, color, color,
        )
    }

    private fun fillQuad(
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
