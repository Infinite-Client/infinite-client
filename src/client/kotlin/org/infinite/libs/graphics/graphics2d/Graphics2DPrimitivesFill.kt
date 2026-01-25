package org.infinite.libs.graphics.graphics2d

import org.infinite.libs.graphics.graphics2d.structs.FillRule
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2DProvider
import org.infinite.libs.graphics.graphics2d.system.Path2D
import kotlin.math.abs
import kotlin.math.atan2

class Graphics2DPrimitivesFill(
    private val provider: RenderCommand2DProvider,
    private val getFillStyle: () -> Int,
    private val getFillRule: () -> FillRule,
) {
    private val fillStyle: Int get() = getFillStyle()
    private val fillRule: FillRule get() = getFillRule()

    // --- 再利用のための一時バッファ群 (Alloc回避) ---
    private val tempVertices = ArrayList<Vertex>(16)
    private val pathVerticesBuffer = ArrayList<Vec2Color>(64)
    private val triangleBuffer = ArrayList<Array<Vec2Color>>(128)
    private val intersectionResultBuffer = ArrayList<Vec2Color>(128)

    data class Vec2Color(var x: Float = 0f, var y: Float = 0f, var color: Int = 0)
    data class Vertex(val x: Float, val y: Float, val color: Int)

    fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        provider.getFillRect().set(x, y, width, height, fillStyle, fillStyle, fillStyle, fillStyle)
    }

    fun fillRect(x: Float, y: Float, width: Float, height: Float, color0: Int, color1: Int, color2: Int, color3: Int) {
        provider.getFillRect()
            .set(x, y, width, height, color0, color1, color2, color3)
    }

    fun fillQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        provider.getFillQuad().set(x0, y0, x1, y1, x2, y2, x3, y3, fillStyle, fillStyle, fillStyle, fillStyle)
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
        tempVertices.clear()
        tempVertices.add(Vertex(x0, y0, col0))
        tempVertices.add(Vertex(x1, y1, col1))
        tempVertices.add(Vertex(x2, y2, col2))
        tempVertices.add(Vertex(x3, y3, col3))

        val cx = (x0 + x1 + x2 + x3) / 4f
        val cy = (y0 + y1 + y2 + y3) / 4f

        tempVertices.sortBy { atan2((it.y - cy).toDouble(), (it.x - cx).toDouble()) }

        provider.getFillQuad().set(
            tempVertices[0].x, tempVertices[0].y,
            tempVertices[1].x, tempVertices[1].y,
            tempVertices[2].x, tempVertices[2].y,
            tempVertices[3].x, tempVertices[3].y,
            tempVertices[0].color, tempVertices[1].color, tempVertices[2].color, tempVertices[3].color,
        )
    }

    fun fillTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, c0: Int, c1: Int, c2: Int) {
        val crossProduct = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0)
        if (crossProduct > 0) {
            provider.getFillTriangle().set(x0, y0, x2, y2, x1, y1, c0, c2, c1)
        } else {
            provider.getFillTriangle().set(x0, y0, x1, y1, x2, y2, c0, c1, c2)
        }
    }

    fun fillPath(path: Path2D) {
        triangleBuffer.clear()

        for (subPath in path.getSubPaths()) {
            val pts = subPath.points
            if (pts.size < 3) continue

            pathVerticesBuffer.clear()
            for (p in pts) {
                pathVerticesBuffer.add(Vec2Color(p.x, p.y, p.style.color))
            }

            if (pathVerticesBuffer.first() != pathVerticesBuffer.last()) {
                val f = pathVerticesBuffer.first()
                pathVerticesBuffer.add(Vec2Color(f.x, f.y, f.color))
            }

            val resolved = resolveSelfIntersections(pathVerticesBuffer)
            val tris = earClipping(resolved)

            for (tri in tris) {
                val midX = (tri[0].x + tri[1].x + tri[2].x) / 3f
                val midY = (tri[0].y + tri[1].y + tri[2].y) / 3f
                if (isInside(midX, midY, path)) {
                    triangleBuffer.add(tri)
                }
            }
        }
        renderOptimizedTriangles(triangleBuffer)
    }

    private fun renderOptimizedTriangles(triangles: ArrayList<Array<Vec2Color>>) {
        if (triangles.isEmpty()) return
        val used = BooleanArray(triangles.size)

        for (i in triangles.indices) {
            if (used[i]) continue
            val triA = triangles[i]
            var merged = false

            for (j in i + 1 until triangles.size) {
                if (used[j]) continue
                val triB = triangles[j]
                val shared = findSharedEdge(triA, triB)
                if (shared != null) {
                    val (idxA1, idxA2, idxBex) = shared
                    val otherA = (0..2).first { it != idxA1 && it != idxA2 }

                    fillQuad(
                        triA[idxA1].x, triA[idxA1].y,
                        triA[otherA].x, triA[otherA].y,
                        triA[idxA2].x, triA[idxA2].y,
                        triB[idxBex].x, triB[idxBex].y,
                        triA[idxA1].color, triA[otherA].color, triA[idxA2].color, triB[idxBex].color,
                    )

                    used[i] = true
                    used[j] = true
                    merged = true
                    break
                }
            }

            if (!merged) {
                fillTriangle(
                    triA[0].x, triA[0].y, triA[1].x, triA[1].y, triA[2].x, triA[2].y,
                    triA[0].color, triA[1].color, triA[2].color,
                )
                used[i] = true
            }
        }
    }

    private fun resolveSelfIntersections(path: List<Vec2Color>): List<Vec2Color> {
        intersectionResultBuffer.clear()
        intersectionResultBuffer.addAll(path)

        var iterations = 0
        val maxIterations = intersectionResultBuffer.size * 2

        var i = 0
        while (i < intersectionResultBuffer.size - 1 && iterations < maxIterations) {
            var j = i + 2
            while (j < intersectionResultBuffer.size - 1) {
                val p1 = intersectionResultBuffer[i]
                val p2 = intersectionResultBuffer[i + 1]
                val p3 = intersectionResultBuffer[j]
                val p4 = intersectionResultBuffer[j + 1]

                if (p1 == p3 || p1 == p4 || p2 == p3 || p2 == p4) {
                    j++
                    continue
                }

                val intersect = findIntersection(p1, p2, p3, p4)
                if (intersect != null) {
                    if (isNear(intersect, p1) || isNear(intersect, p2) ||
                        isNear(intersect, p3) || isNear(intersect, p4)
                    ) {
                        j++
                        continue
                    }

                    intersectionResultBuffer.add(i + 1, intersect)
                    intersectionResultBuffer.add(j + 2, intersect)
                    iterations++
                    break
                }
                j++
            }
            i++
        }
        return intersectionResultBuffer
    }

    private fun findSharedEdge(a: Array<Vec2Color>, b: Array<Vec2Color>): Triple<Int, Int, Int>? {
        val shared = mutableListOf<Pair<Int, Int>>()
        for (i in 0..2) {
            for (j in 0..2) {
                if (abs(a[i].x - b[j].x) < 1e-4f && abs(a[i].y - b[j].y) < 1e-4f) {
                    shared.add(i to j)
                }
            }
        }
        if (shared.size == 2) {
            val bUsed = shared.map { it.second }
            val bExtra = (0..2).first { it !in bUsed }
            return Triple(shared[0].first, shared[1].first, bExtra)
        }
        return null
    }

    /**
     * Winding Number または Ray Casting を使用して内外判定を行います。
     */
    private fun isInside(x: Float, y: Float, path: Path2D): Boolean {
        var windingNumber = 0
        var crossingCount = 0

        for (sub in path.getSubPaths()) {
            val pts = sub.points
            for (i in pts.indices) {
                val p1 = pts[i]
                val p2 = pts[(i + 1) % pts.size]

                // Even-Odd (Ray Casting)
                if (((p1.y <= y && y < p2.y) || (p2.y <= y && y < p1.y)) &&
                    (x < (p2.x - p1.x) * (y - p1.y) / (p2.y - p1.y) + p1.x)
                ) {
                    crossingCount++
                }

                // Non-Zero (Winding Number)
                if (p1.y <= y) {
                    if (p2.y > y && crossProduct(
                            Vec2Color(p1.x, p1.y, 0),
                            Vec2Color(p2.x, p2.y, 0),
                            Vec2Color(x, y, 0),
                        ) > 0
                    ) {
                        windingNumber++
                    }
                } else {
                    if (p2.y <= y && crossProduct(
                            Vec2Color(p1.x, p1.y, 0),
                            Vec2Color(p2.x, p2.y, 0),
                            Vec2Color(x, y, 0),
                        ) < 0
                    ) {
                        windingNumber--
                    }
                }
            }
        }

        return when (fillRule) {
            FillRule.EvenOdd -> (crossingCount % 2 != 0)
            FillRule.NonZero -> (windingNumber != 0)
        }
    }

    private fun isNear(v1: Vec2Color, v2: Vec2Color): Boolean = abs(v1.x - v2.x) < 0.01f && abs(v1.y - v2.y) < 0.01f

    /**
     * 耳切法の安全性を強化
     */
    private fun earClipping(vertices: List<Vec2Color>): List<Array<Vec2Color>> {
        if (vertices.size < 3) return emptyList()
        val triangles = mutableListOf<Array<Vec2Color>>()
        val work = vertices.distinct().toMutableList() // 重複頂点を削除

        if (calculateArea(work) < 0) work.reverse()

        // 頂点数の3倍以上のループが回ったら強制終了（無限ループ防止）
        var watchdog = work.size * 3
        while (work.size > 3 && watchdog > 0) {
            var earFound = false
            for (i in work.indices) {
                val prev = work[(i + work.size - 1) % work.size]
                val curr = work[i]
                val next = work[(i + 1) % work.size]

                if (isEar(prev, curr, next, work)) {
                    triangles.add(arrayOf(prev, curr, next))
                    work.removeAt(i)
                    earFound = true
                    break
                }
            }
            if (!earFound) break
            watchdog--
        }

        if (work.size == 3) triangles.add(arrayOf(work[0], work[1], work[2]))
        return triangles
    }

    private fun findIntersection(p1: Vec2Color, p2: Vec2Color, p3: Vec2Color, p4: Vec2Color): Vec2Color? {
        val det = (p2.x - p1.x) * (p4.y - p3.y) - (p2.y - p1.y) * (p4.x - p3.x)
        if (abs(det) < 1e-6) return null // 平行

        val t = ((p3.x - p1.x) * (p4.y - p3.y) - (p3.y - p1.y) * (p4.x - p3.x)) / det
        val u = ((p3.x - p1.x) * (p2.y - p1.y) - (p3.y - p1.y) * (p2.x - p1.x)) / det

        return if (t in 0.0..1.0 && u in 0.0..1.0) {
            val ix = p1.x + t * (p2.x - p1.x)
            val iy = p1.y + t * (p2.y - p1.y)
            // 色の線形補間
            val icolor = lerpColor(p1.color, p2.color, t)
            Vec2Color(ix, iy, icolor)
        } else {
            null
        }
    }

    private fun isEar(p1: Vec2Color, p2: Vec2Color, p3: Vec2Color, polygon: List<Vec2Color>): Boolean {
        // 凸判定
        if (crossProduct(p1, p2, p3) <= 0) return false
        // 他の頂点がこの三角形の中に含まれていないか
        for (p in polygon) {
            if (p == p1 || p == p2 || p == p3) continue
            if (isPointInTriangle(p, p1, p2, p3)) return false
        }
        return true
    }

    private fun lerpColor(c1: Int, c2: Int, t: Float): Int {
        val a = (((c1 shr 24) and 0xFF) * (1 - t) + ((c2 shr 24) and 0xFF) * t).toInt()
        val r = (((c1 shr 16) and 0xFF) * (1 - t) + ((c2 shr 16) and 0xFF) * t).toInt()
        val g = (((c1 shr 8) and 0xFF) * (1 - t) + ((c2 shr 8) and 0xFF) * t).toInt()
        val b = ((c1 and 0xFF) * (1 - t) + (c2 and 0xFF) * t).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun crossProduct(a: Vec2Color, b: Vec2Color, c: Vec2Color) = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

    private fun calculateArea(p: List<Vec2Color>) = p.indices.sumOf { i ->
        val j = (i + 1) % p.size
        (p[i].x * p[j].y - p[j].x * p[i].y).toDouble()
    } / 2.0

    private fun isPointInTriangle(p: Vec2Color, a: Vec2Color, b: Vec2Color, c: Vec2Color): Boolean {
        val d1 = crossProduct(p, a, b)
        val d2 = crossProduct(p, b, c)
        val d3 = crossProduct(p, c, a)
        val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
        val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)
        return !(hasNeg && hasPos)
    }
}
