package org.infinite.libs.graphics.graphics2d

import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2DProvider
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.graphics.graphics2d.system.PointPair

class Graphics2DPrimitivesStroke(
    private val provider: RenderCommand2DProvider,
    private val getStrokeStyle: () -> StrokeStyle?,
) {
    private val strokeStyle: StrokeStyle? get() = getStrokeStyle()

    fun strokeRect(x: Float, y: Float, width: Float, height: Float) {
        val style = strokeStyle ?: return
        strokeRect(x, y, width, height, style.color, style.color, style.color, style.color)
    }

    fun strokeRect(x: Float, y: Float, w: Float, h: Float, col0: Int, col1: Int, col2: Int, col3: Int) {
        val style = strokeStyle ?: return
        val hw = style.width / 2f

        val p0 = PointPair(x + hw, y + hw, x - hw, y - hw)
        val p1 = PointPair(x + w - hw, y + hw, x + w + hw, y - hw)
        val p2 = PointPair(x + w - hw, y + h - hw, x + w + hw, y + h + hw)
        val p3 = PointPair(x + hw, y + h - hw, x - hw, y + h + hw)

        // listOf を使わず直接描画
        drawColoredEdge(p0, p1, col0, col1, col0, col1)
        drawColoredEdge(p1, p2, col1, col2, col1, col2)
        drawColoredEdge(p2, p3, col2, col3, col2, col3)
        drawColoredEdge(p3, p0, col3, col0, col3, col0)
    }

    fun strokeQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val style = strokeStyle ?: return
        strokeQuad(x0, y0, x1, y1, x2, y2, x3, y3, style.color, style.color, style.color, style.color)
    }

    fun strokeQuad(
        ix0: Float,
        iy0: Float,
        ix1: Float,
        iy1: Float,
        ix2: Float,
        iy2: Float,
        ix3: Float,
        iy3: Float,
        icol0: Int,
        icol1: Int,
        icol2: Int,
        icol3: Int,
    ) {
        val style = strokeStyle ?: return
        val hw = style.width / 2f

        // 反時計回り正規化 (正規化関数の戻り値もバッファ化を推奨)
        val q = normalizeToCCW(ix0, iy0, ix1, iy1, ix2, iy2, ix3, iy3, icol0, icol1, icol2, icol3)

        val p0 = PointPair.calculateForMiter(q.x0, q.y0, q.x3, q.y3, q.x1, q.y1, hw)
        val p1 = PointPair.calculateForMiter(q.x1, q.y1, q.x0, q.y0, q.x2, q.y2, hw)
        val p2 = PointPair.calculateForMiter(q.x2, q.y2, q.x1, q.y1, q.x3, q.y3, hw)
        val p3 = PointPair.calculateForMiter(q.x3, q.y3, q.x2, q.y2, q.x0, q.y0, hw)

        // QuadColorSampler 内のリスト生成も排除済みと仮定
        val inner = QuadColorSampler.sample(
            p0.ix, p0.iy, p1.ix, p1.iy, p2.ix, p2.iy, p3.ix, p3.iy,
            q.x0, q.y0, q.x1, q.y1, q.x2, q.y2, q.x3, q.y3, q.c0, q.c1, q.c2, q.c3,
        )

        drawColoredEdge(p0, p1, q.c0, q.c1, inner[0], inner[1])
        drawColoredEdge(p1, p2, q.c1, q.c2, inner[1], inner[2])
        drawColoredEdge(p2, p3, q.c2, q.c3, inner[2], inner[3])
        drawColoredEdge(p3, p0, q.c3, q.c0, inner[3], inner[0])
    }

    fun drawColoredEdge(start: PointPair, end: PointPair, outSCol: Int, outECol: Int, inSCol: Int, inECol: Int) {
        // provider.getFillQuad().set() を使用
        provider.getFillQuad().set(
            start.ox, start.oy,
            end.ox, end.oy,
            end.ix, end.iy,
            start.ix, start.iy,
            outSCol, outECol, inECol, inSCol,
        )
    }

    fun strokeTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        val style = strokeStyle ?: return
        strokeTriangle(x0, y0, x1, y1, x2, y2, style.color, style.color, style.color)
    }

    fun strokeTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, col0: Int, col1: Int, col2: Int) {
        val style = strokeStyle ?: return
        val hw = style.width / 2f

        // 1. 各角のマイタージョイント座標を計算
        val p0 = PointPair.calculateForMiter(x0, y0, x2, y2, x1, y1, hw)
        val p1 = PointPair.calculateForMiter(x1, y1, x0, y0, x2, y2, hw)
        val p2 = PointPair.calculateForMiter(x2, y2, x1, y1, x0, y0, hw)

        // 2. 内側の色を決定 (Triple を使わず直接変数へ)
        val inCol0: Int
        val inCol1: Int
        val inCol2: Int

        if (style.width > 2.0f) {
            inCol0 = lerpColorInTriangle(p0.ix, p0.iy, x0, y0, x1, y1, x2, y2, col0, col1, col2)
            inCol1 = lerpColorInTriangle(p1.ix, p1.iy, x0, y0, x1, y1, x2, y2, col0, col1, col2)
            inCol2 = lerpColorInTriangle(p2.ix, p2.iy, x0, y0, x1, y1, x2, y2, col0, col1, col2)
        } else {
            inCol0 = col0
            inCol1 = col1
            inCol2 = col2
        }

        // 3. エッジの描画 (3つの FillQuad コマンドが Provider 経由で生成される)
        drawColoredEdge(p0, p1, col0, col1, inCol0, inCol1)
        drawColoredEdge(p1, p2, col1, col2, inCol1, inCol2)
        drawColoredEdge(p2, p0, col2, col0, inCol2, inCol0)
    }
}
