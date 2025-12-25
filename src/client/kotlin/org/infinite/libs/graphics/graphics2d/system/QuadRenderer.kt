package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fillQuad
import org.infinite.libs.graphics.graphics2d.system.PointPair.Companion.calculateForMiter
import kotlin.math.abs

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
                // 四角形を2つの三角形 (0,1,2) と (0,2,3) に分割して判定
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
            // 外積を用いた包含判定（方向を一貫させる）
            fun crossProduct(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) =
                (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)

            val d1 = crossProduct(px, py, x0, y0, x1, y1)
            val d2 = crossProduct(px, py, x1, y1, x2, y2)
            val d3 = crossProduct(px, py, x2, y2, x0, y0)

            val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
            val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)
            return !(hasNeg && hasPos)
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
            if (abs(denom) < 1e-6f) return c0

            val w0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / denom
            val w1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / denom
            val w2 = 1f - w0 - w1

            // 頂点カラーをRGBA成分に分解（安全なLong/Float変換）
            fun extract(c: Int) = floatArrayOf(
                (c shr 24 and 0xFF).toFloat(),
                (c shr 16 and 0xFF).toFloat(),
                (c shr 8 and 0xFF).toFloat(),
                (c and 0xFF).toFloat(),
            )

            val v0 = extract(c0)
            val v1 = extract(c1)
            val v2 = extract(c2)

            val a = (v0[0] * w0 + v1[0] * w1 + v2[0] * w2).toInt().coerceIn(0, 255)
            val r = (v0[1] * w0 + v1[1] * w1 + v2[1] * w2).toInt().coerceIn(0, 255)
            val g = (v0[2] * w0 + v1[2] * w1 + v2[2] * w2).toInt().coerceIn(0, 255)
            val b = (v0[3] * w0 + v1[3] * w1 + v2[3] * w2).toInt().coerceIn(0, 255)

            return (a shl 24) or (r shl 16) or (g shl 8) or b
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
        val q = normalizeToCCW(x0, y0, x1, y1, x2, y2, x3, y3, col0, col1, col2, col3)
        guiGraphics.fillQuad(q.x0, q.y0, q.x1, q.y1, q.x2, q.y2, q.x3, q.y3, q.c0, q.c1, q.c2, q.c3)
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
        strokeWidth: Float,
    ) {
        // 1. 反時計回りに正規化
        val q = normalizeToCCW(ix0, iy0, ix1, iy1, ix2, iy2, ix3, iy3, icol0, icol1, icol2, icol3)

        val hw = strokeWidth / 2f

        // 2. 正規化された座標で計算
        val p0 = calculateForMiter(q.x0, q.y0, q.x3, q.y3, q.x1, q.y1, hw)
        val p1 = calculateForMiter(q.x1, q.y1, q.x0, q.y0, q.x2, q.y2, hw)
        val p2 = calculateForMiter(q.x2, q.y2, q.x1, q.y1, q.x3, q.y3, hw)
        val p3 = calculateForMiter(q.x3, q.y3, q.x2, q.y2, q.x0, q.y0, hw)

        // 3. 内側の色をサンプリング
        val innerCols = if (strokeWidth > 2.0f) {
            QuadColorSampler.sample(
                p0.ix, p0.iy, p1.ix, p1.iy, p2.ix, p2.iy, p3.ix, p3.iy,
                q.x0, q.y0, q.x1, q.y1, q.x2, q.y2, q.x3, q.y3,
                q.c0, q.c1, q.c2, q.c3,
            )
        } else {
            listOf(q.c0, q.c1, q.c2, q.c3)
        }

        // 4. エッジ描画 (色の引数順序を修正)
        // 引数: start, end, outSCol, outECol, inSCol, inECol
        drawColoredEdge(p0, p1, q.c0, q.c1, innerCols[0], innerCols[1])
        drawColoredEdge(p1, p2, q.c1, q.c2, innerCols[1], innerCols[2])
        drawColoredEdge(p2, p3, q.c2, q.c3, innerCols[2], innerCols[3])
        drawColoredEdge(p3, p0, q.c3, q.c0, innerCols[3], innerCols[0])
    }

    private fun drawColoredEdge(
        start: PointPair,
        end: PointPair,
        outSCol: Int,
        outECol: Int,
        inSCol: Int,
        inECol: Int,
    ) {
        // 頂点指定順序:
        // 1: 開始外(ox,oy) -> 2: 終了外(ox,oy) -> 3: 終了内(ix,iy) -> 4: 開始内(ix,iy)
        guiGraphics.fillQuad(
            start.ox, start.oy,
            end.ox, end.oy,
            end.ix, end.iy,
            start.ix, start.iy,
            outSCol, // 1に対応
            outECol, // 2に対応
            inECol, // 3に対応 (終了地点の内側の色)
            inSCol, // 4に対応 (開始地点の内側の色)
        )
    }

    /**
     * 頂点と色のペアを保持するデータ構造
     */
    private data class NormalizedQuad(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val x3: Float,
        val y3: Float,
        val c0: Int,
        val c1: Int,
        val c2: Int,
        val c3: Int,
    )

    /**
     * 頂点の順序を反時計回り(CCW)に正規化し、対応する色も入れ替える
     */
    private fun normalizeToCCW(
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
    ): NormalizedQuad {
        // 符号付き面積の計算 (Shoelace formula)
        // MinecraftのGUI座標系（下が正）では、この値が正なら時計回り(CW)
        val area = (x1 - x0) * (y1 + y0) +
            (x2 - x1) * (y2 + y1) +
            (x3 - x2) * (y3 + y2) +
            (x0 - x3) * (y0 + y3)

        return if (area < 0) {
            // 時計回りなので、頂点1と頂点3を入れ替えて反時計回りにする (0 -> 3 -> 2 -> 1)
            NormalizedQuad(x0, y0, x3, y3, x2, y2, x1, y1, c0, c3, c2, c1)
        } else {
            // 既に反時計回り
            NormalizedQuad(x0, y0, x1, y1, x2, y2, x3, y3, c0, c1, c2, c3)
        }
    }
}
