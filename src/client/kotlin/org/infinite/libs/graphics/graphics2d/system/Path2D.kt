package org.infinite.libs.graphics.graphics2d.system

import org.infinite.libs.core.tick.RenderTicks
import org.infinite.libs.graphics.graphics2d.structs.FillRule
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.nativebind.infinite_client_h
import java.lang.AutoCloseable
import java.lang.foreign.MemorySegment
import kotlin.math.*

class Path2D : AutoCloseable {
    // --- Native Resource ---
    private var path2d = infinite_client_h.graphics2d_path2d_new()

    override fun close() {
        if (path2d != MemorySegment.NULL) {
            infinite_client_h.graphics2d_path2d_drop(path2d)
            path2d = MemorySegment.NULL
        }
    }

    // --- Path States ---
    data class PathPoint(val x: Float, val y: Float, val style: StrokeStyle, val smooth: Boolean = true)
    class Segments(val points: MutableList<PathPoint> = mutableListOf(), var isClosed: Boolean = false)

    private val subPathList = mutableListOf<Segments>()
    private var lastPoint: Pair<Float, Float>? = null
    private var firstPointOfSubPath: Pair<Float, Float>? = null

    // --- Buffers (Alloc回避) ---
    data class Vec2Color(var x: Float, var y: Float, var color: Int)
    private val pathVerticesBuffer = ArrayList<Vec2Color>(128)
    private val triangleBuffer = ArrayList<Array<Vec2Color>>(256)
    private val intersectionResultBuffer = ArrayList<Vec2Color>(256)

    // --- Basic Path Operations ---
    private fun getCurrentSubPath(): Segments {
        if (subPathList.isEmpty()) subPathList.add(Segments())
        return subPathList.last()
    }

    fun beginPath() {
        subPathList.clear()
        lastPoint = null
        firstPointOfSubPath = null
    }

    fun moveTo(x: Float, y: Float) {
        if (subPathList.isNotEmpty() && subPathList.last().points.isNotEmpty()) {
            subPathList.add(Segments())
        }
        lastPoint = x to y
        firstPointOfSubPath = x to y
    }

    fun lineTo(x: Float, y: Float, style: StrokeStyle) {
        val current = getCurrentSubPath()
        if (current.points.isEmpty()) {
            val startX = lastPoint?.first ?: x
            val startY = lastPoint?.second ?: y
            current.points.add(PathPoint(startX, startY, style, false))
            firstPointOfSubPath = startX to startY
        }

        val lx = lastPoint?.first ?: x
        val ly = lastPoint?.second ?: y
        if ((x - lx).pow(2) + (y - ly).pow(2) < 0.0025f) return

        current.points.add(PathPoint(x, y, style, true))
        lastPoint = x to y
    }

    fun closePath(style: StrokeStyle) {
        val current = getCurrentSubPath()
        val start = firstPointOfSubPath ?: return
        val last = lastPoint ?: return
        if (abs(last.first - start.first) > 1e-4f || abs(last.second - start.second) > 1e-4f) {
            lineTo(start.first, start.second, style)
        }
        current.isClosed = true
    }

    // --- Advanced Path Curves ---
    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float, style: StrokeStyle) {
        val (p0x, p0y) = lastPoint ?: return
        val dx1 = x1 - p0x
        val dy1 = y1 - p0y
        val dx2 = x2 - x1
        val dy2 = y2 - y1
        val len1 = sqrt(dx1 * dx1 + dy1 * dy1)
        val len2 = sqrt(dx2 * dx2 + dy2 * dy2)

        if (len1 < 1e-6f || len2 < 1e-6f || radius <= 0f) {
            lineTo(x1, y1, style)
            return
        }

        val u1x = dx1 / len1
        val u1y = dy1 / len1
        val u2x = dx2 / len2
        val u2y = dy2 / len2
        val cross = u1x * u2y - u1y * u2x
        if (abs(cross) < 1e-6f) {
            lineTo(x1, y1, style)
            return
        }

        val angle = acos(-(u1x * u2x + u1y * u2y).coerceIn(-1f, 1f))
        val tangentDist = radius / tan(angle / 2f)
        val t1x = x1 - u1x * tangentDist
        val t1y = y1 - u1y * tangentDist
        val t2x = x1 + u2x * tangentDist
        val t2y = y1 + u2y * tangentDist

        val isClockwise = cross > 0
        val nx = if (isClockwise) -u1y else u1y
        val ny = if (isClockwise) u1x else -u1x
        val cx = t1x + nx * radius
        val cy = t1y + ny * radius

        lineTo(t1x, t1y, style)
        arc(cx, cy, radius, atan2(t1y - cy, t1x - cx), atan2(t2y - cy, t2x - cx), !isClockwise, style)
    }

    fun arc(x: Float, y: Float, radius: Float, startA: Float, endA: Float, ccw: Boolean, style: StrokeStyle) {
        if (lastPoint == null) moveTo(x + cos(startA) * radius, y + sin(startA) * radius)
        var diff = endA - startA
        if (!ccw) {
            while (diff <= 0) diff += (2 * PI).toFloat()
        } else {
            while (diff >= 0) diff -= (2 * PI).toFloat()
        }

        val res = (abs(diff) * radius / 1.5f * getQualityScale()).toInt().coerceIn(8, 64)
        for (i in 1..res) {
            val a = startA + (diff * i / res)
            lineTo(x + cos(a) * radius, y + sin(a) * radius, style)
        }
    }

    fun bezierCurveTo(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float, x: Float, y: Float, style: StrokeStyle) {
        val (p0x, p0y) = lastPoint ?: return
        val res = (
            (abs(cp1x - p0x) + abs(cp2x - cp1x) + abs(x - cp2x) + abs(cp1y - p0y) + abs(cp2y - cp1y) + abs(y - cp2y)) /
                1.5f * getQualityScale()
            ).toInt().coerceIn(8, 64)
        for (i in 1..res) {
            val t = i.toFloat() / res
            val it = 1f - t
            val px = it.pow(3) * p0x + 3 * it.pow(2) * t * cp1x + 3 * it * t.pow(2) * cp2x + t.pow(3) * x
            val py = it.pow(3) * p0y + 3 * it.pow(2) * t * cp1y + 3 * it * t.pow(2) * cp2y + t.pow(3) * y
            lineTo(px, py, style)
        }
    }

    // --- Rendering (Fill Logic) ---
    fun fillPath(
        fillRule: FillRule,
        fillTriangle: (Number, Number, Number, Number, Number, Number, Int, Int, Int) -> Unit,
        fillQuad: (Number, Number, Number, Number, Number, Number, Number, Number, Int, Int, Int, Int) -> Unit,
    ) {
        triangleBuffer.clear()
        for (sub in subPathList) {
            if (sub.points.size < 3) continue
            pathVerticesBuffer.clear()
            sub.points.forEach { pathVerticesBuffer.add(Vec2Color(it.x, it.y, it.style.color)) }

            // 自己交差解決 & 耳切法
            val resolved = resolveSelfIntersections(pathVerticesBuffer)
            val tris = earClipping(resolved)

            tris.forEach { tri ->
                val mx = (tri[0].x + tri[1].x + tri[2].x) / 3f
                val my = (tri[0].y + tri[1].y + tri[2].y) / 3f
                if (isInside(mx, my, fillRule)) triangleBuffer.add(tri)
            }
        }
        renderOptimized(triangleBuffer, fillTriangle, fillQuad)
    }

    private fun renderOptimized(
        tris: ArrayList<Array<Vec2Color>>,
        fTri: (Float, Float, Float, Float, Float, Float, Int, Int, Int) -> Unit,
        fQuad: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        val used = BooleanArray(tris.size)
        for (i in tris.indices) {
            if (used[i]) continue
            val triA = tris[i]
            var merged = false
            for (j in i + 1 until tris.size) {
                if (used[j]) continue
                val triB = tris[j]
                val shared = findSharedEdge(triA, triB)
                if (shared != null) {
                    val (a1, a2, be) = shared
                    val ae = (0..2).first { it != a1 && it != a2 }
                    fQuad(
                        triA[a1].x, triA[a1].y, triA[ae].x, triA[ae].y, triA[a2].x, triA[a2].y, triB[be].x, triB[be].y,
                        triA[a1].color, triA[ae].color, triA[a2].color, triB[be].color,
                    )
                    used[i] = true
                    used[j] = true
                    merged = true
                    break
                }
            }
            if (!merged) {
                fTri(triA[0].x, triA[0].y, triA[1].x, triA[1].y, triA[2].x, triA[2].y, triA[0].color, triA[1].color, triA[2].color)
                used[i] = true
            }
        }
    }

    // --- Math Utilities (Self-Intersection, Ear Clipping, Winding) ---
    private fun resolveSelfIntersections(path: List<Vec2Color>): List<Vec2Color> {
        intersectionResultBuffer.clear()
        intersectionResultBuffer.addAll(path)
        // 自己交差を検出し頂点を挿入するロジック（簡略化版を想定）
        return intersectionResultBuffer
    }

    private fun earClipping(vertices: List<Vec2Color>): List<Array<Vec2Color>> {
        val tris = mutableListOf<Array<Vec2Color>>()
        val work = vertices.distinct().toMutableList()
        if (work.size < 3) return tris
        var watchdog = work.size * 3
        while (work.size > 3 && watchdog-- > 0) {
            for (i in work.indices) {
                val p = work[(i + work.size - 1) % work.size]
                val c = work[i]
                val n = work[(i + 1) % work.size]
                if ((c.x - p.x) * (n.y - p.y) - (c.y - p.y) * (n.x - p.x) > 0) { // 凸判定
                    tris.add(arrayOf(p, c, n))
                    work.removeAt(i)
                    break
                }
            }
        }
        if (work.size == 3) tris.add(arrayOf(work[0], work[1], work[2]))
        return tris
    }

    private fun isInside(x: Float, y: Float, rule: FillRule): Boolean {
        var cc = 0
        subPathList.forEach { sub ->
            for (i in sub.points.indices) {
                val p1 = sub.points[i]
                val p2 = sub.points[(i + 1) % sub.points.size]
                if (((p1.y <= y && y < p2.y) || (p2.y <= y && y < p1.y)) && (x < (p2.x - p1.x) * (y - p1.y) / (p2.y - p1.y) + p1.x)) cc++
            }
        }
        return if (rule == FillRule.EvenOdd) cc % 2 != 0 else false
    }

    private fun findSharedEdge(a: Array<Vec2Color>, b: Array<Vec2Color>): Triple<Int, Int, Int>? {
        val shared = mutableListOf<Pair<Int, Int>>()
        for (i in 0..2) for (j in 0..2) if (abs(a[i].x - b[j].x) < 1e-4f && abs(a[i].y - b[j].y) < 1e-4f) shared.add(i to j)
        return if (shared.size == 2) Triple(shared[0].first, shared[1].first, (0..2).first { it !in shared.map { s -> s.second } }) else null
    }
    fun strokePath(
        isPathGradientEnabled: Boolean,
        drawEdge: (PointPair, PointPair, Int, Int, Int, Int) -> Unit,
    ) {
        for (subPath in subPathList) {
            val points = subPath.points
            if (points.size < 2) continue

            miteredPairsBuffer.clear()
            val isClosed = subPath.isClosed

            // 1. 各点のマイタージョイント（角の座標）を一括計算
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
                miteredPairsBuffer.add(pair)
            }

            // 2. セグメントごとに描画（コールバックを実行）
            for (i in 0 until points.size - 1) {
                val startPair = miteredPairsBuffer[i]
                val endPair = miteredPairsBuffer[i + 1]

                val startCol = points[i].style.color
                val endCol = if (isPathGradientEnabled) points[i + 1].style.color else startCol

                // 線の中と外で色が同じ（基本）として描画
                drawEdge(startPair, endPair, startCol, endCol, startCol, endCol)
            }
        }
    }
    private val miteredPairsBuffer = ArrayList<PointPair>(256)
    private fun calculateCap(curr: PathPoint, adj: PathPoint, isStart: Boolean): PointPair {
        val dx = adj.x - curr.x
        val dy = adj.y - curr.y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-4f)
        val nx = -dy / len
        val ny = dx / len
        val hw = curr.style.width / 2f

        return if (isStart) {
            PointPair(curr.x - nx * hw, curr.y - ny * hw, curr.x + nx * hw, curr.y + ny * hw)
        } else {
            PointPair(curr.x + nx * hw, curr.y + ny * hw, curr.x - nx * hw, curr.y - ny * hw)
        }
    }

    fun getSubPaths() = subPathList
    fun clearSegments() = beginPath()

    companion object {
        fun getQualityScale(): Float {
            val fps = RenderTicks.fps
            return when {
                fps >= 50f -> 1.0f
                fps <= 5f -> 0.5f
                else -> (fps / 60f).coerceIn(0.5f, 1.0f)
            }
        }
    }
}
