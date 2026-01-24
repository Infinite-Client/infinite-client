package org.infinite.libs.graphics.graphics2d.system

import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import kotlin.math.*

class Path2D {
    /**
     * 座標、その点に適用されるスタイル、および接続属性を保持するデータクラス
     */
    data class PathPoint(
        val x: Float,
        val y: Float,
        val style: StrokeStyle,
        val smooth: Boolean = true,
    )

    /**
     * 一続きの線（サブパス）の集合を保持するクラス
     */
    class Segments {
        val points = mutableListOf<PathPoint>()
        var isClosed: Boolean = false

        fun isEmpty() = points.isEmpty()
    }

    private val subPathList = mutableListOf<Segments>()
    private var lastPoint: Pair<Float, Float>? = null
    private var firstPointOfSubPath: Pair<Float, Float>? = null

    /**
     * 現在編集中のサブパスを取得。存在しない場合は新規作成。
     */
    private fun getCurrentSubPath(): Segments {
        if (subPathList.isEmpty()) {
            subPathList.add(Segments())
        }
        return subPathList.last()
    }

    /**
     * パスを完全に初期化します。
     */
    fun beginPath() {
        subPathList.clear()
        lastPoint = null
        firstPointOfSubPath = null
    }

    /**
     * 新しいサブパスを開始し、ペンを指定した座標に移動させます。
     */
    fun moveTo(x: Float, y: Float) {
        // 現在のサブパスが空でないなら、新しいサブパスを追加して切り分ける
        if (subPathList.isNotEmpty() && !subPathList.last().isEmpty()) {
            subPathList.add(Segments())
        }
        lastPoint = x to y
        firstPointOfSubPath = x to y
    }

    /**
     * 現在のペン位置から指定した座標まで直線を追加します。
     */
    fun lineTo(x: Float, y: Float, style: StrokeStyle) {
        val current = getCurrentSubPath()

        // 1. 始点の処理
        if (current.points.isEmpty()) {
            val startX = lastPoint?.first ?: x
            val startY = lastPoint?.second ?: y
            current.points.add(PathPoint(startX, startY, style, smooth = false))
            firstPointOfSubPath = startX to startY
            lastPoint = startX to startY
        }

        // 2. --- 重要：近すぎる点をスキップ ---
        // 前の点との距離が 0.05ピクセル 以下の場合は無視する
        // これにより、PointPair でのゼロ除算や数値破綻を根本から防ぎます
        val (lx, ly) = lastPoint!!
        val distSq = (x - lx) * (x - lx) + (y - ly) * (y - ly)
        if (distSq < 0.0025f) return // 0.05 * 0.05 = 0.0025

        current.points.add(PathPoint(x, y, style, smooth = true))
        lastPoint = x to y
    }

    /**
     * 現在のサブパスを閉じ、始点と終点を結合します。
     */
    fun closePath(style: StrokeStyle) {
        val current = getCurrentSubPath()
        val start = firstPointOfSubPath ?: return
        val last = lastPoint ?: return

        // 終点が始点と重なっていない場合は、線を繋ぐ
        if (abs(last.first - start.first) > 1e-4f || abs(last.second - start.second) > 1e-4f) {
            lineTo(start.first, start.second, style)
        }
        current.isClosed = true
    }

    /**
     * 円弧を追加します。
     */
    fun arc(
        x: Float,
        y: Float,
        radius: Float,
        startAngle: Float,
        endAngle: Float,
        counterclockwise: Boolean = false,
        style: StrokeStyle,
    ) {
        if (lastPoint == null) moveTo(x + cos(startAngle) * radius, y + sin(startAngle) * radius)

        val twoPi = 2 * PI.toFloat()
        var diff = endAngle - startAngle

        if (!counterclockwise) {
            while (diff <= 0) diff += twoPi
        } else {
            while (diff >= 0) diff -= twoPi
        }

        // --- 修正ポイント：半径に基づいた動的分割 ---
        // 半径が小さい（例: 4px）なら分割を少なく、大きいなら多くする
        // 目安として「1ピクセルあたりの弦の長さ」が一定になるようにします
        val circumference = abs(diff) * radius
        val segmentsCount = (circumference / 2.0f).toInt().coerceIn(4, 64)

        val step = diff / segmentsCount

        for (i in 1..segmentsCount) {
            val a = startAngle + step * i
            lineTo(x + cos(a) * radius, y + sin(a) * radius, style)
        }
    }

    /**
     * 接線を利用した円弧を追加します。
     */
    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float, style: StrokeStyle) {
        val p0 = lastPoint ?: return
        val (p0x, p0y) = p0

        // ベクトル p0->p1 と p1->p2
        val dx1 = x1 - p0x
        val dy1 = y1 - p0y
        val dx2 = x2 - x1
        val dy2 = y2 - y1

        val len1 = sqrt(dx1 * dx1 + dy1 * dy1)
        val len2 = sqrt(dx2 * dx2 + dy2 * dy2)

        // 直線上の場合やサイズゼロの場合は単純な直線として処理
        if (len1 < 1e-6f || len2 < 1e-6f || radius <= 0f) {
            lineTo(x1, y1, style)
            return
        }

        // 単位ベクトル
        val u1x = dx1 / len1
        val u1y = dy1 / len1
        val u2x = dx2 / len2
        val u2y = dy2 / len2

        // 外積で回転方向を判定 (Canvas座標系はY軸下向き)
        val cross = u1x * u2y - u1y * u2x
        if (abs(cross) < 1e-6f) { // 直線状
            lineTo(x1, y1, style)
            return
        }

        // 内角 theta の計算
        val cosTheta = -(u1x * u2x + u1y * u2y) // 反対向きのベクトルとの内積
        val angle = acos(cosTheta.coerceIn(-1f, 1f))
        val tangentDist = radius / tan(angle / 2f)

        // 接点 T1, T2
        val t1x = x1 - u1x * tangentDist
        val t1y = y1 - u1y * tangentDist
        val t2x = x1 + u2x * tangentDist
        val t2y = y1 + u2y * tangentDist

        // 中心点 C
        // 接点 T1 からの法線方向を求める
        val isClockwise = cross > 0
        val nx = if (isClockwise) -u1y else u1y
        val ny = if (isClockwise) u1x else -u1x

        val cx = t1x + nx * radius
        val cy = t1y + ny * radius

        // arc() に渡すための角度算出
        val startA = atan2(t1y - cy, t1x - cx)
        val endA = atan2(t2y - cy, t2x - cx)

        // 1. 始点から最初の接点まで直線
        lineTo(t1x, t1y, style)

        // 2. 円弧部分
        // arc内部で counterclockwise を正しく扱う
        arc(cx, cy, radius, startA, endA, !isClockwise, style)
    }

    /**
     * ベジェ曲線を追加します。
     */
    fun bezierCurveTo(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float, x: Float, y: Float, style: StrokeStyle) {
        val p0 = lastPoint ?: return
        val (p0x, p0y) = p0

        // 1. 曲線の「概算長さ」を計算（コントロールポイント間の直線距離の合計）
        // 厳密な積分は重いため、L1ノルム（マンハッタン距離）に近い近似で十分です
        val d01 = abs(cp1x - p0x) + abs(cp1y - p0y)
        val d12 = abs(cp2x - cp1x) + abs(cp2y - cp1y)
        val d23 = abs(x - cp2x) + abs(y - cp2y)
        val estimatedLength = d01 + d12 + d23

        // 2. 長さに応じて分割数を決定
        // 2ピクセルごとに1分割を基準にし、最低4、最大64分割程度に制限します
        val resolution = (estimatedLength / 2.0f).toInt().coerceIn(4, 64)

        for (i in 1..resolution) {
            val t = i.toFloat() / resolution
            val it = 1f - t

            // 3次ベジェ曲線の数式: (1-t)^3*P0 + 3(1-t)^2*t*P1 + 3(1-t)*t^2*P2 + t^3*P3
            val ptx = it * it * it * p0x +
                3 * it * it * t * cp1x +
                3 * it * t * t * cp2x +
                t * t * t * x

            val pty = it * it * it * p0y +
                3 * it * it * t * cp1y +
                3 * it * t * t * cp2y +
                t * t * t * y

            // lineTo 内部ですでに「近すぎる点のスキップ」が実装されているため、
            // ここで生成された点も自動的に最適化されます
            lineTo(ptx, pty, style)
        }
    }

    fun getSubPaths(): List<Segments> = subPathList

    fun clearSegments() = beginPath()
    // Path2D.kt 内

    /**
     * 現在のパス状態を複製します
     */
    fun snapshot(): List<Segments> = subPathList.map { seg ->
        Segments().apply {
            this.points.addAll(seg.points)
            this.isClosed = seg.isClosed
        }
    }

    /**
     * 保存されたパス状態を復元します
     */
    fun restore(snapshot: List<Segments>, last: Pair<Float, Float>?, first: Pair<Float, Float>?) {
        subPathList.clear()
        subPathList.addAll(snapshot)
        this.lastPoint = last
        this.firstPointOfSubPath = first
    }

    // 内部変数へのアクセス用（snapshot時に必要）
    val lastPointData get() = lastPoint
    val firstPointData get() = firstPointOfSubPath
}
