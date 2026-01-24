package org.infinite.libs.graphics.graphics2d

import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.graphics.graphics2d.system.Path2D
import org.infinite.libs.graphics.graphics2d.system.PointPair
import java.util.*
import kotlin.math.sqrt

class Graphics2DPrimitivesStroke(
    private val commandQueue: LinkedList<RenderCommand2D>,
    private val getStrokeStyle: () -> StrokeStyle?, // Lambda to get current strokeStyle from Graphics2D
    private val enablePathGradient: () -> Boolean, // Lambda to get enablePathGradient from Graphics2D
) {
    private val strokeStyle: StrokeStyle? get() = getStrokeStyle()
    private val isPathGradientEnabled: Boolean get() = enablePathGradient()

    fun strokeRect(x: Float, y: Float, width: Float, height: Float) {
        val style = strokeStyle ?: return
        strokeRect(x, y, width, height, style.color, style.color, style.color, style.color)
    }

    fun strokeRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        col0: Int,
        col1: Int,
        col2: Int,
        col3: Int,
    ) {
        val style = strokeStyle ?: return
        val hw = style.width / 2f

        // 4角の座標ペアを定義 (ix, iy は内側 / ox, oy は外側)
        val p0 = PointPair(x + hw, y + hw, x - hw, y - hw) // 左上
        val p1 = PointPair(x + w - hw, y + hw, x + w + hw, y - hw) // 右上
        val p2 = PointPair(x + w - hw, y + h - hw, x + w + hw, y + h + hw) // 右下
        val p3 = PointPair(x + hw, y + h - hw, x - hw, y + h + hw) // 左下

        // エッジのリストを作成 (開始点, 終了点, 開始色, 終了色)
        val edges = listOf(
            Triple(p0, p1, col0 to col1),
            Triple(p1, p2, col1 to col2),
            Triple(p2, p3, col2 to col3),
            Triple(p3, p0, col3 to col0),
        )

        for ((start, end, cols) in edges) {
            drawColoredEdge(start, end, cols.first, cols.second, cols.first, cols.second)
        }
    }
    fun strokeQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val style = strokeStyle ?: return
        val color = style.color

        strokeQuad(x0, y0, x1, y1, x2, y2, x3, y3, color, color, color, color)
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
        val strokeWidth = style.width

        // 1. 反時計回りに正規化
        val q = normalizeToCCW(ix0, iy0, ix1, iy1, ix2, iy2, ix3, iy3, icol0, icol1, icol2, icol3)

        val hw = strokeWidth / 2f

        // 2. 正規化された座標で計算
        val p0 = PointPair.calculateForMiter(q.x0, q.y0, q.x3, q.y3, q.x1, q.y1, hw)
        val p1 = PointPair.calculateForMiter(q.x1, q.y1, q.x0, q.y0, q.x2, q.y2, hw)
        val p2 = PointPair.calculateForMiter(q.x2, q.y2, q.x1, q.y1, q.x3, q.y3, hw)
        val p3 = PointPair.calculateForMiter(q.x3, q.y3, q.x2, q.y2, q.x0, q.y0, hw)

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
        commandQueue.add(
            RenderCommand2D.FillQuad(
                start.ox, start.oy,
                end.ox, end.oy,
                end.ix, end.iy,
                start.ix, start.iy,
                outSCol, // 1に対応
                outECol, // 2に対応
                inECol, // 3に対応 (終了地点の内側の色)
                inSCol, // 4に対応 (開始地点の内側の色)
            ),
        )
    }

    fun strokeTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        val style = strokeStyle ?: return
        val color = style.color

        strokeTriangle(x0, y0, x1, y1, x2, y2, color, color, color)
    }

    fun strokeTriangle(
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
        val style = strokeStyle ?: return
        val strokeWidth = style.width
        val hw = strokeWidth / 2f

        // 1. 各角のオフセット座標を計算
        val p0 = PointPair.calculateForMiter(x0, y0, x2, y2, x1, y1, hw)
        val p1 = PointPair.calculateForMiter(x1, y1, x0, y0, x2, y2, hw)
        val p2 = PointPair.calculateForMiter(x2, y2, x1, y1, x0, y0, hw)

        // 2. 内側の色を決定
        val (inCol0, inCol1, inCol2) = if (strokeWidth > 2.0f) {
            Triple(
                lerpColorInTriangle(p0.ix, p0.iy, x0, y0, x1, y1, x2, y2, col0, col1, col2),
                lerpColorInTriangle(p1.ix, p1.iy, x0, y0, x1, y1, x2, y2, col0, col1, col2),
                lerpColorInTriangle(p2.ix, p2.iy, x0, y0, x1, y1, x2, y2, col0, col1, col2),
            )
        } else {
            // 幅が狭い場合は、元の頂点色をそのまま使う（高速）
            Triple(col0, col1, col2)
        }

        // 3. 描画
        drawColoredEdge(p0, p1, inCol0, inCol1, col0, col1)
        drawColoredEdge(p1, p2, inCol1, inCol2, col1, col2)
        drawColoredEdge(p2, p0, inCol2, inCol0, col2, col0)
    }

    fun strokePath(path: Path2D) {
        for (subPath in path.getSubPaths()) {
            if (subPath.points.size < 2) continue
            renderSubPath(subPath)
        }
    }

    private fun renderSubPath(subPath: Path2D.Segments) {
        val points = subPath.points
        val isClosed = subPath.isClosed
        val miteredPairs = mutableListOf<PointPair>()

        // 1. 全てのジョイント座標を先に計算 (計算方法は前回提供したものでOK)
        for (i in points.indices) {
            val curr = points[i]
            val hw = curr.style.width / 2f

            val pair = if (isClosed) {
                val prevIdx = if (i == 0) points.size - 2 else i - 1
                val nextIdx = if (i == points.size - 1) 1 else i + 1
                PointPair.calculateForMiter(curr.x, curr.y, points[prevIdx].x, points[prevIdx].y, points[nextIdx].x, points[nextIdx].y, hw)
            } else {
                when (i) {
                    0 -> calculateCap(curr, points[1], true)
                    points.size - 1 -> calculateCap(curr, points[i - 1], false)
                    else -> PointPair.calculateForMiter(curr.x, curr.y, points[i - 1].x, points[i - 1].y, points[i + 1].x, points[i + 1].y, hw)
                }
            }
            miteredPairs.add(pair)
        }

        // 2. 描画 (三角形の隙間を埋めるため、接続部を意識)
        for (i in 0 until points.size - 1) {
            val startPair = miteredPairs[i]
            val endPair = miteredPairs[i + 1]

            // グラデーション設定
            val startCol = points[i].style.color
            val endCol = if (isPathGradientEnabled) points[i + 1].style.color else startCol

            // 座標が 0.01px 単位でズレると太さが変わって見えるため、
            // drawColoredEdge 内で渡す頂点を再確認
            drawColoredEdge(startPair, endPair, startCol, endCol, startCol, endCol)
        }
    }
    private fun calculateCap(curr: Path2D.PathPoint, adj: Path2D.PathPoint, isStart: Boolean): PointPair {
        val dx = adj.x - curr.x
        val dy = adj.y - curr.y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        val nx = -dy / len
        val ny = dx / len
        val hw = curr.style.width / 2f

        return if (isStart) {
            PointPair(curr.x - nx * hw, curr.y - ny * hw, curr.x + nx * hw, curr.y + ny * hw)
        } else {
            // 終点では法線を反転させ、進行方向に対して右側が ox になるように維持
            PointPair(curr.x + nx * hw, curr.y + ny * hw, curr.x - nx * hw, curr.y - ny * hw)
        }
    }
}
