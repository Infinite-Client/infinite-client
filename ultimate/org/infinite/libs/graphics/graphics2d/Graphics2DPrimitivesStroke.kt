package org.infinite.libs.graphics.graphics2d

import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.graphics.graphics2d.system.PathSegment
import org.infinite.libs.graphics.graphics2d.system.PointPair
import java.util.LinkedList

class Graphics2DPrimitivesStroke(
    private val commandQueue: LinkedList<RenderCommand>,
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
        col0: Int, // 左上
        col1: Int, // 右上
        col2: Int, // 右下
        col3: Int, // 左下
    ) {
        val style = strokeStyle ?: return
        val strokeWidth = style.width
        val v = strokeWidth / 2f
        val p1 = PointPair(x + v, y + v, x - v, y - v)
        val p2 = PointPair(x + v, y + h - v, x - v, y + h + v)
        val p3 = PointPair(x + w - v, y + h - v, x + w + v, y + h + v)
        val p4 = PointPair(x + w - v, y + v, x + w + v, y - v)

        commandQueue.add(
            RenderCommand.FillQuad(
                p1.ix,
                p1.iy,
                p1.ox,
                p1.oy,
                p2.ox,
                p2.oy,
                p2.ix,
                p2.iy,
                col0,
                col0,
                col1,
                col1,
            ),
        )
        commandQueue.add(
            RenderCommand.FillQuad(
                p2.ix,
                p2.iy,
                p2.ox,
                p2.oy,
                p3.ox,
                p3.oy,
                p3.ix,
                p3.iy,
                col1,
                col1,
                col2,
                col2,
            ),
        )
        commandQueue.add(
            RenderCommand.FillQuad(
                p3.ix,
                p3.iy,
                p3.ox,
                p3.oy,
                p4.ox,
                p4.oy,
                p4.ix,
                p4.iy,
                col2,
                col2,
                col3,
                col3,
            ),
        )
        commandQueue.add(
            RenderCommand.FillQuad(
                p4.ix,
                p4.iy,
                p4.ox,
                p4.oy,
                p1.ox,
                p1.oy,
                p1.ix,
                p1.iy,
                col3,
                col3,
                col0,
                col0,
            ),
        )
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
            RenderCommand.FillQuad(
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

    fun strokePolyline(segments: List<PathSegment>) {
        if (segments.isEmpty()) return

        val polylineVerticesWithStyles = mutableListOf<Triple<Float, Float, StrokeStyle>>()
        if (segments.isNotEmpty()) {
            polylineVerticesWithStyles.add(Triple(segments.first().x1, segments.first().y1, segments.first().style))
            for (segment in segments) {
                polylineVerticesWithStyles.add(Triple(segment.x2, segment.y2, segment.style))
            }
        } else {
            return // No segments to draw
        }

        val isClosed = segments.size > 1 &&
            segments.first().x1 == segments.last().x2 &&
            segments.first().y1 == segments.last().y2

        val miteredPoints = mutableListOf<PointPair>()

        for (j in 0 until polylineVerticesWithStyles.size) {
            val currV = polylineVerticesWithStyles[j]
            val currX = currV.first
            val currY = currV.second
            val currSegmentStyle = currV.third // The style of the segment *starting* at this vertex

            val prevX: Float
            val prevY: Float
            val nextX: Float
            val nextY: Float
            val halfWidthForMiter: Float

            if (isClosed) {
                val prevVIndex = if (j == 0) polylineVerticesWithStyles.size - 2 else j - 1
                val nextVIndex = if (j == polylineVerticesWithStyles.size - 1) 1 else j + 1

                val prevSegmentStyle = polylineVerticesWithStyles[prevVIndex].third

                prevX = polylineVerticesWithStyles[prevVIndex].first
                prevY = polylineVerticesWithStyles[prevVIndex].second
                nextX = polylineVerticesWithStyles[nextVIndex].first
                nextY = polylineVerticesWithStyles[nextVIndex].second

                halfWidthForMiter = (prevSegmentStyle.width + currSegmentStyle.width) / 4f
            } else { // Open path
                if (j == 0) { // First vertex of the polyline (start cap)
                    val firstSegment = segments.first()
                    prevX = currX - (firstSegment.x2 - firstSegment.x1)
                    prevY = currY - (firstSegment.y2 - firstSegment.y1)
                    nextX = polylineVerticesWithStyles[j + 1].first
                    nextY = polylineVerticesWithStyles[j + 1].second
                    halfWidthForMiter = currSegmentStyle.width / 2f
                } else if (j == polylineVerticesWithStyles.size - 1) { // Last vertex of the polyline (end cap)
                    val lastSegment = segments.last()
                    prevX = polylineVerticesWithStyles[j - 1].first
                    prevY = polylineVerticesWithStyles[j - 1].second
                    nextX = currX + (lastSegment.x2 - lastSegment.x1)
                    nextY = currY + (lastSegment.y2 - lastSegment.y1)
                    halfWidthForMiter = currSegmentStyle.width / 2f
                } else { // Intermediate vertex (joint)
                    prevX = polylineVerticesWithStyles[j - 1].first
                    prevY = polylineVerticesWithStyles[j - 1].second
                    nextX = polylineVerticesWithStyles[j + 1].first
                    nextY = polylineVerticesWithStyles[j + 1].second

                    val prevSegmentStyle = polylineVerticesWithStyles[j - 1].third
                    halfWidthForMiter = (prevSegmentStyle.width + currSegmentStyle.width) / 4f
                }
            }

            miteredPoints.add(
                PointPair.calculateForMiter(
                    currX,
                    currY,
                    prevX,
                    prevY,
                    nextX,
                    nextY,
                    halfWidthForMiter,
                ),
            )
        }

        val numSegmentsToDraw = segments.size
        for (j in 0 until numSegmentsToDraw) {
            val segment = segments[j]
            val startMiter = miteredPoints[j]
            val endMiter = if (isClosed && j == segments.size - 1) miteredPoints[0] else miteredPoints[j + 1]
            val style = segment.style

            val finalStartColor: Int
            val finalEndColor: Int

            if (isPathGradientEnabled) {
                // Determine start and end colors for gradient
                finalStartColor = style.color // Color of current segment's start
                if (j + 1 < numSegmentsToDraw) {
                    finalEndColor = segments[j + 1].style.color // Color of next segment's start
                } else if (isClosed) {
                    finalEndColor = segments.first().style.color // For closed path, last segment connects to first
                } else {
                    finalEndColor = style.color // For open path, last segment uses its own color
                }
            } else {
                finalStartColor = style.color
                finalEndColor = style.color
            }

            drawColoredEdge(startMiter, endMiter, finalStartColor, finalEndColor, finalStartColor, finalEndColor)
        }
    }
}
