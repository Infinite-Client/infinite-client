package org.infinite.utils.rendering

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

data class Quad(val vertex1: Vec3, val vertex2: Vec3, val vertex3: Vec3, val vertex4: Vec3, val color: Int, val normal: Vector3f)
data class Line(val start: Vec3, val end: Vec3, val color: Int)
data class BlockMesh(val quads: List<Quad>, val lines: List<Line>)

object BlockMeshGenerator {
    fun generateMesh(blockPositions: Map<BlockPos, Int>): BlockMesh {
        if (blockPositions.isEmpty()) return BlockMesh(emptyList(), emptyList())

        val finalQuads = mutableListOf<Quad>()
        val rawLines = mutableListOf<Line>()

        // --- 1. 面の生成と結合 (Greedy Quad Merging) ---
        Direction.entries.forEach { dir ->
            val normal = Vector3f(dir.stepX.toFloat(), dir.stepY.toFloat(), dir.stepZ.toFloat())

            // 面が必要な位置を収集 (カリング済)
            val facePositions = mutableMapOf<Int, MutableMap<Int, MutableSet<Pair<Int, Int>>>>()
            blockPositions.forEach { (pos, color) ->
                if (blockPositions[pos.relative(dir)] != color) {
                    val (plane, u, v) = getCoords(pos, dir)
                    facePositions.getOrPut(color) { mutableMapOf() }
                        .getOrPut(plane) { mutableSetOf() }
                        .add(u to v)
                }
            }

            // 平面・色ごとに2D Greedy Meshingを実行
            facePositions.forEach { (color, planes) ->
                planes.forEach { (plane, cells) ->
                    finalQuads.addAll(greedyMesh2D(plane, color, cells, dir, normal))
                }
            }
        }

        // --- 2. 辺の生成 (カリング込み) ---
        val uniqueLines = mutableSetOf<Pair<Vec3, Vec3>>()
        blockPositions.forEach { (pos, color) ->
            // 各軸(X, Y, Z)の周辺4エッジをチェック
            processEdgesForPos(rawLines, uniqueLines, blockPositions, pos, color)
        }

        return BlockMesh(finalQuads, combineLines(rawLines))
    }

    private fun getCoords(pos: BlockPos, dir: Direction): Triple<Int, Int, Int> = when (dir.axis) {
        Direction.Axis.X -> Triple(pos.x, pos.z, pos.y)
        Direction.Axis.Y -> Triple(pos.y, pos.x, pos.z)
        Direction.Axis.Z -> Triple(pos.z, pos.x, pos.y)
    }

    private fun greedyMesh2D(plane: Int, color: Int, cells: MutableSet<Pair<Int, Int>>, dir: Direction, normal: Vector3f): List<Quad> {
        val quads = mutableListOf<Quad>()
        while (cells.isNotEmpty()) {
            val start = cells.first()
            var width = 1
            while (cells.contains(start.first + width to start.second)) width++

            var height = 1
            while (true) {
                var allRow = true
                for (w in 0 until width) {
                    if (!cells.contains(start.first + w to start.second + height)) {
                        allRow = false
                        break
                    }
                }
                if (allRow) height++ else break
            }

            // 頂点の構築
            quads.add(buildQuad(plane, start.first, start.second, width, height, color, dir, normal))

            // 使用済みセルを削除
            for (w in 0 until width) {
                for (h in 0 until height) {
                    cells.remove(start.first + w to start.second + h)
                }
            }
        }
        return quads
    }

    private fun buildQuad(p: Int, u: Int, v: Int, w: Int, h: Int, color: Int, dir: Direction, normal: Vector3f): Quad {
        val offset = if (dir.axisDirection == Direction.AxisDirection.POSITIVE) 1.0 else 0.0
        val plane = p.toDouble() + offset

        val v1: Vec3
        val v2: Vec3
        val v3: Vec3
        val v4: Vec3

        when (dir.axis) {
            Direction.Axis.X -> {
                if (dir.axisDirection == Direction.AxisDirection.POSITIVE) {
                    v1 = Vec3(plane, v.toDouble(), u.toDouble())
                    v2 = Vec3(plane, v.toDouble() + h, u.toDouble())
                    v3 = Vec3(plane, v.toDouble() + h, u.toDouble() + w)
                    v4 = Vec3(plane, v.toDouble(), u.toDouble() + w)
                } else {
                    v1 = Vec3(plane, v.toDouble(), u.toDouble())
                    v2 = Vec3(plane, v.toDouble(), u.toDouble() + w)
                    v3 = Vec3(plane, v.toDouble() + h, u.toDouble() + w)
                    v4 = Vec3(plane, v.toDouble() + h, u.toDouble())
                }
            }

            Direction.Axis.Y -> {
                if (dir.axisDirection == Direction.AxisDirection.POSITIVE) {
                    v1 = Vec3(u.toDouble(), plane, v.toDouble())
                    v2 = Vec3(u.toDouble(), plane, v.toDouble() + h)
                    v3 = Vec3(u.toDouble() + w, plane, v.toDouble() + h)
                    v4 = Vec3(u.toDouble() + w, plane, v.toDouble())
                } else {
                    v1 = Vec3(u.toDouble(), plane, v.toDouble())
                    v2 = Vec3(u.toDouble() + w, plane, v.toDouble())
                    v3 = Vec3(u.toDouble() + w, plane, v.toDouble() + h)
                    v4 = Vec3(u.toDouble(), plane, v.toDouble() + h)
                }
            }

            Direction.Axis.Z -> {
                if (dir.axisDirection == Direction.AxisDirection.POSITIVE) {
                    v1 = Vec3(u.toDouble(), v.toDouble(), plane)
                    v2 = Vec3(u.toDouble() + w, v.toDouble(), plane)
                    v3 = Vec3(u.toDouble() + w, v.toDouble() + h, plane)
                    v4 = Vec3(u.toDouble(), v.toDouble() + h, plane)
                } else {
                    v1 = Vec3(u.toDouble(), v.toDouble(), plane)
                    v2 = Vec3(u.toDouble(), v.toDouble() + h, plane)
                    v3 = Vec3(u.toDouble() + w, v.toDouble() + h, plane)
                    v4 = Vec3(u.toDouble() + w, v.toDouble(), plane)
                }
            }
        }
        return Quad(v1, v2, v3, v4, color, normal)
    }

    private fun processEdgesForPos(ls: MutableList<Line>, unq: MutableSet<Pair<Vec3, Vec3>>, blocks: Map<BlockPos, Int>, pos: BlockPos, color: Int) {
        val x = pos.x.toDouble()
        val y = pos.y.toDouble()
        val z = pos.z.toDouble()
        // 各方向のエッジを抽象化してチェック
        val edgeCheck = { x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, n1: BlockPos, n2: BlockPos ->
            val c1 = blocks[n1]
            val c2 = blocks[n2]
            if (c1 != color || c2 != color) {
                val s = Vec3(x1, y1, z1)
                val e = Vec3(x2, y2, z2)
                val pair = if (s.x < e.x || (s.x == e.x && (s.y < e.y || (s.y == e.y && s.z < e.z)))) s to e else e to s
                if (unq.add(pair)) {
                    ls.add(
                        Line(
                            s,
                            e,
                            if (c1 != null && c1 != color) {
                                interpolate(color, c1)
                            } else if (c2 != null && c2 != color) {
                                interpolate(color, c2)
                            } else {
                                color
                            },
                        ),
                    )
                }
            }
        }
        // X-axis edges
        edgeCheck(x, y, z, x + 1, y, z, pos.below(), pos.north())
        edgeCheck(x, y + 1, z, x + 1, y + 1, z, pos.above(), pos.north())
        edgeCheck(x, y, z + 1, x + 1, y, z + 1, pos.below(), pos.south())
        edgeCheck(x, y + 1, z + 1, x + 1, y + 1, z + 1, pos.above(), pos.south())
        // Y-axis
        edgeCheck(x, y, z, x, y + 1, z, pos.west(), pos.north())
        edgeCheck(x + 1, y, z, x + 1, y + 1, z, pos.east(), pos.north())
        edgeCheck(x, y, z + 1, x, y + 1, z + 1, pos.west(), pos.south())
        edgeCheck(x + 1, y, z + 1, x + 1, y + 1, z + 1, pos.east(), pos.south())
        // Z-axis
        edgeCheck(x, y, z, x, y, z + 1, pos.west(), pos.below())
        edgeCheck(x + 1, y, z, x + 1, y, z + 1, pos.east(), pos.below())
        edgeCheck(x, y + 1, z, x, y + 1, z + 1, pos.west(), pos.above())
        edgeCheck(x + 1, y + 1, z, x + 1, y + 1, z + 1, pos.east(), pos.above())
    }

    private fun combineLines(lines: List<Line>): List<Line> {
        val result = mutableListOf<Line>()
        val axes = listOf(
            { l: Line -> Triple(l.start.y, l.start.z, l.color) } to { l: Line -> l.start.x },
            { l: Line -> Triple(l.start.x, l.start.z, l.color) } to { l: Line -> l.start.y },
            { l: Line -> Triple(l.start.x, l.start.y, l.color) } to { l: Line -> l.start.z },
        )
        axes.forEach { (grouper, sorter) ->
            lines.filter {
                when (axes.indexOf(grouper to sorter)) {
                    0 -> it.start.y == it.end.y && it.start.z == it.end.z
                    1 -> it.start.x == it.end.x && it.start.z == it.end.z
                    else -> it.start.x == it.end.x && it.start.y == it.end.y
                }
            }.groupBy(grouper).forEach { (_, list) ->
                val sorted = list.sortedBy(sorter)
                var curS = sorted[0].start
                var curE = sorted[0].end
                var curC = sorted[0].color
                for (i in 1 until sorted.size) {
                    if (sorted[i].start == curE && sorted[i].color == curC) {
                        curE = sorted[i].end
                    } else {
                        result.add(Line(curS, curE, curC))
                        curS = sorted[i].start
                        curE = sorted[i].end
                        curC = sorted[i].color
                    }
                }
                result.add(Line(curS, curE, curC))
            }
        }
        return result
    }

    private fun interpolate(c1: Int, c2: Int): Int {
        fun avg(s1: Int, s2: Int) = (s1 + s2) / 2
        return (avg(c1 shr 24 and 0xFF, c2 shr 24 and 0xFF) shl 24) or (avg(c1 shr 16 and 0xFF, c2 shr 16 and 0xFF) shl 16) or (avg(c1 shr 8 and 0xFF, c2 shr 8 and 0xFF) shl 8) or avg(c1 and 0xFF, c2 and 0xFF)
    }
}
