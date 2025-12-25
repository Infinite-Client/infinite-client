package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fillQuad
import org.infinite.libs.graphics.graphics2d.system.PointPair.Companion.calculateForMiter

class QuadRenderer(private val guiGraphics: GuiGraphics) {
    object QuadColorSampler {
        fun sample(
            ix0: Float,
            iy0: Float,
            ix1: Float,
            iy1: Float,
            ix2: Float,
            iy2: Float,
            ix3: Float,
            iy3: Float,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
            x3: Float,
            y3: Float,
            c0: Int,
            c1: Int,
            c2: Int,
            c3: Int,
        ): List<Int> {
            val pts = arrayOf(ix0 to iy0, ix1 to iy1, ix2 to iy2, ix3 to iy3)
            return pts.map { (px, py) ->
                // 点(px, py)が、三角形(0,1,2)か(0,2,3)のどちらに含まれるかで補間
                if (isPointInTriangle(px, py, x0, y0, x1, y1, x2, y2)) {
                    lerpColor(px, py, x0, y0, x1, y1, x2, y2, c0, c1, c2)
                } else {
                    lerpColor(px, py, x0, y0, x2, y2, x3, y3, c0, c2, c3)
                }
            }
        }

        private fun isPointInTriangle(
            px: Float,
            py: Float,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
        ): Boolean {
            val b1 = (px - x1) * (y0 - y1) - (x0 - x1) * (py - y1) < 0.0f
            val b2 = (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2) < 0.0f
            val b3 = (px - x0) * (y2 - y0) - (x2 - x0) * (py - y0) < 0.0f
            return (b1 == b2) && (b2 == b3)
        }

        private fun lerpColor(
            px: Float,
            py: Float,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
            c0: Int,
            c1: Int,
            c2: Int,
        ): Int {
            val denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if (denom == 0f) return c0
            val w0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / denom
            val w1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / denom
            val w2 = 1f - w0 - w1

            val a = (c0 ushr 24) * w0 + (c1 ushr 24) * w1 + (c2 ushr 24) * w2
            val r = ((c0 shr 16) and 0xFF) * w0 + ((c1 shr 16) and 0xFF) * w1 + ((c2 shr 16) and 0xFF) * w2
            val g = ((c0 shr 8) and 0xFF) * w0 + ((c1 shr 8) and 0xFF) * w1 + ((c2 shr 8) and 0xFF) * w2
            val b = (c0 and 0xFF) * w0 + (c1 and 0xFF) * w1 + (c2 and 0xFF) * w2
            return (a.toInt() shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }
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
        val hw = strokeWidth / 2f

        // 各頂点に対して、前後の頂点情報を渡してオフセット計算
        val p0 = calculateForMiter(x0, y0, x3, y3, x1, y1, hw)
        val p1 = calculateForMiter(x1, y1, x0, y0, x2, y2, hw)
        val p2 = calculateForMiter(x2, y2, x1, y1, x3, y3, hw)
        val p3 = calculateForMiter(x3, y3, x2, y2, x0, y0, hw)

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

    fun strokeQuad(
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
        strokeWidth: Float,
    ) {
        val hw = strokeWidth / 2f

        // 1. 各頂点のオフセット座標計算
        val p0 = calculateForMiter(x0, y0, x3, y3, x1, y1, hw)
        val p1 = calculateForMiter(x1, y1, x0, y0, x2, y2, hw)
        val p2 = calculateForMiter(x2, y2, x1, y1, x3, y3, hw)
        val p3 = calculateForMiter(x3, y3, x2, y2, x0, y0, hw)

        // 2. 内側の色を決定 (外側は常に指定された colN を使用)
        val (inCol0, inCol1, inCol2, inCol3) = if (strokeWidth > 2.0f) {
            QuadColorSampler.sample(
                p0.ix, p0.iy, p1.ix, p1.iy, p2.ix, p2.iy, p3.ix, p3.iy,
                x0, y0, x1, y1, x2, y2, x3, y3,
                col0, col1, col2, col3,
            )
        } else {
            listOf(col0, col1, col2, col3)
        }

        // 3. 4つの辺を描画 (外側: 頂点色 colN / 内側: サンプル色 inColN)
        drawColoredEdge(p0, p1, col0, col1, inCol0, inCol1)
        drawColoredEdge(p1, p2, col1, col2, inCol1, inCol2)
        drawColoredEdge(p2, p3, col2, col3, inCol2, inCol3)
        drawColoredEdge(p3, p0, col3, col0, inCol3, inCol0)
    }

    /**
     * @param outSCol 開始外側色, @param outECol 終了外側色
     * @param inSCol 開始内側色, @param inECol 終了内側色
     */
    private fun drawColoredEdge(
        start: PointPair,
        end: PointPair,
        outSCol: Int,
        outECol: Int,
        inSCol: Int,
        inECol: Int,
    ) {
        guiGraphics.fillQuad(
            start.ox, start.oy,
            end.ox, end.oy,
            end.ix, end.iy,
            start.ix, start.iy,
            outSCol, outECol, inECol, inSCol,
        )
    }
}
