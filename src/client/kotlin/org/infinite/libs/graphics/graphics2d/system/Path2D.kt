package org.infinite.libs.graphics.graphics2d.system

import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import java.util.LinkedList
import kotlin.math.*

/**
 * MDN CanvasRenderingContext2D API のパス描画に関する機能を再現するクラス。
 * パスデータを保持し、パスの操作ロジックを提供します。
 */
class Path2D(
    private val commandQueue: LinkedList<RenderCommand>,
) {
    private var currentPath: MutableList<Pair<Float, Float>> = mutableListOf()
    private var startPath: Pair<Float, Float>? = null

    /**
     * 新しいパスを開始します。既存のパスはリセットされます。
     */
    fun beginPath() {
        currentPath.clear()
        startPath = null
    }

    /**
     * ペンの現在位置を指定された座標に移動します。
     * パスの開始点も設定されます。
     */
    fun moveTo(x: Float, y: Float) {
        currentPath.add(x to y)
        if (startPath == null) {
            startPath = x to y
        }
    }

    /**
     * 現在のパスに指定された座標までの線を追加します。
     */
    fun lineTo(x: Float, y: Float) {
        currentPath.add(x to y)
    }

    /**
     * パスを閉じます。パスの開始点と現在位置を直線で結びます。
     * パスが閉じられた後も、現在のペン位置は最後の点に留まります。
     */
    fun closePath() {
        startPath?.let {
            if (currentPath.lastOrNull() != it) {
                currentPath.add(it)
            }
        }
    }

    /**
     * 現在のパスを、現在のストロークスタイルで描画します。
     * 描画後、パスはクリアされます。
     */
    fun strokePath(strokeWidth: Float, strokeColor: Int) {
        if (currentPath.size < 2) return

        // パスを線分に分解して描画
        for (i in 0 until currentPath.size - 1) {
            val p1 = currentPath[i]
            val p2 = currentPath[i + 1]

            val x1 = p1.first
            val y1 = p1.second
            val x2 = p2.first
            val y2 = p2.second

            if (strokeWidth <= 0) continue

            val dx = x2 - x1
            val dy = y2 - y1
            val length = sqrt(dx * dx + dy * dy)

            if (length == 0f) continue

            val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
            val halfWidth = strokeWidth / 2.0f

            val nx = -sin(angle) // 法線ベクトルのx成分
            val ny = cos(angle) // 法線ベクトルのy成分

            // 線の四隅の座標を計算
            val p1x_quad = x1 + nx * halfWidth
            val p1y_quad = y1 + ny * halfWidth
            val p2x_quad = x2 + nx * halfWidth
            val p2y_quad = y2 + ny * halfWidth
            val p3x_quad = x2 - nx * halfWidth
            val p3y_quad = y2 - ny * halfWidth
            val p4x_quad = x1 - nx * halfWidth
            val p4y_quad = y1 - ny * halfWidth

            commandQueue.add(
                RenderCommand.FillQuad(
                    p1x_quad, p1y_quad,
                    p2x_quad, p2y_quad,
                    p3x_quad, p3y_quad,
                    p4x_quad, p4y_quad,
                    strokeColor, strokeColor, strokeColor, strokeColor,
                ),
            )
        }
        // パス描画後にパスをクリア
        currentPath.clear()
        startPath = null
    }

    /**
     * 指定された円弧を現在のパスに追加します。
     * @param x 円の中心のX座標
     * @param y 円の中心のY座標
     * @param radius 円の半径
     * @param startAngle 円弧の開始角度（ラジアン）
     * @param endAngle 円弧の終了角度（ラジアン）
     * @param counterclockwise 反時計回りであるかどうか。デフォルトはfalse（時計回り）。
     */
    fun arc(x: Float, y: Float, radius: Float, startAngle: Float, endAngle: Float, counterclockwise: Boolean = false) {
        // 現在のパスが空の場合、moveToで開始点に移動
        if (currentPath.isEmpty()) {
            moveTo(x + cos(startAngle) * radius, y + sin(startAngle) * radius)
        }

        val TWO_PI = 2 * PI.toFloat()
        var currentStartAngle = startAngle
        var currentEndAngle = endAngle

        // 角度を正規化して [0, 2*PI] の範囲に収める
        fun normalizeAngle(angle: Float): Float {
            var norm = angle % TWO_PI
            if (norm < 0) norm += TWO_PI
            return norm
        }

        currentStartAngle = normalizeAngle(currentStartAngle)
        currentEndAngle = normalizeAngle(currentEndAngle)

        // 反時計回りの場合、startAngle > endAngle のときは endAngle に TWO_PI を加算
        // 時計回りの場合、startAngle < currentEndAngle のときは endAngle から TWO_PI を減算
        if (counterclockwise) {
            if (currentStartAngle < currentEndAngle) currentStartAngle += TWO_PI
        } else {
            if (currentStartAngle > currentEndAngle) currentEndAngle -= TWO_PI
        }

        val angleDiff = if (counterclockwise) {
            currentStartAngle - currentEndAngle
        } else {
            currentEndAngle - currentStartAngle
        }

        // セグメント数 (円周率 * 半径 * 2 を基準に、適度な解像度で分割)
        // または、角度差に基づいて調整
        val segments = (angleDiff / (PI / 12)).toInt().coerceAtLeast(2) // 15度ごとに1セグメント

        val angleStep = angleDiff / segments * (if (counterclockwise) -1 else 1)

        for (i in 1..segments) {
            val angle = startAngle + angleStep * i
            lineTo(x + cos(angle) * radius, y + sin(angle) * radius)
        }
    }

    /**
     * 指定された制御点と半径で円弧を現在のパスに追加します。
     * @param x1 制御点1のX座標
     * @param y1 制御点1のY座標
     * @param x2 制御点2のX座標
     * @param y2 制御点2のY座標
     * @param radius 円弧の半径
     */
    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float) {
        if (currentPath.isEmpty()) {
            // 現在のパスが空の場合は何もしない (moveTo(x1, y1) とはしない)
            return
        }

        val p0x = currentPath.last().first
        val p0y = currentPath.last().second

        // ベクトル P0P1 (v1) と P1P2 (v2)
        val v1x = x1 - p0x
        val v1y = y1 - p0y
        val v2x = x2 - x1
        val v2y = y2 - y1

        val len1 = sqrt(v1x * v1x + v1y * v1y)
        val len2 = sqrt(v2x * v2x + v2y * v2y)

        // 長さが0の場合は何もしない
        if (len1 < 1e-6 || len2 < 1e-6) {
            lineTo(x1, y1) // 接点がないので、制御点1まで直線
            return
        }

        // ベクトルを正規化
        val u1x = v1x / len1
        val u1y = v1y / len1
        val u2x = v2x / len2
        val u2y = v2y / len2

        // cos(theta) = u1 ドット u2
        val cosTheta = u1x * u2x + u1y * u2y

        // P0, P1, P2 が同一直線上にある場合、または半径が0の場合、lineTo(x1, y1)
        if (abs(cosTheta - 1.0f) < 1e-6 || radius == 0f) {
            lineTo(x1, y1)
            return
        }

        val theta = acos(cosTheta)
        val tanHalfTheta = tan(theta / 2.0f)

        // 円弧の中心までの距離
        val dist = radius / tanHalfTheta

        // 接点の計算
        val t1x = x1 - u1x * dist
        val t1y = y1 - u1y * dist
        val t2x = x1 + u2x * dist
        val t2y = y1 + u2y * dist

        // 円の中心 O の計算
        // P1 を基準とした P1T1 (u1*dist) と P1T2 (-u2*dist) のなす角の二等分線の垂直方向
        // クロス積 (v1x * v2y - v1y * v2x) の符号でどちら側かを判断
        val crossProduct = u1x * u2y - u1y * u2x // u1からu2への回転方向
        val normalScale = radius / sin(theta / 2.0f)
        val sign = if (crossProduct > 0) 1.0f else -1.0f // 反時計回りなら正、時計回りなら負

        val normalX = (u1y - u2y) * sign
        val normalY = (-u1x + u2x) * sign
        val normalLen = sqrt(normalX * normalX + normalY * normalY)
        val nX = normalX / normalLen
        val nY = normalY / normalLen

        val cx = x1 + nX * normalScale
        val cy = y1 + nY * normalScale

        val startArcAngle = atan2((t1y - cy).toDouble(), (t1x - cx).toDouble()).toFloat()
        val endArcAngle = atan2((t2y - cy).toDouble(), (t2x - cx).toDouble()).toFloat()

        // 描画方向の決定 (MDNのarcToは常に最短経路で描画)
        // 制御点1を基準に、P0 -> P1 -> P2 の順でたどる際に、P1で曲がる方向が円弧の方向になる
        val counterclockwiseArc = crossProduct > 0

        // lineToでt1に接続
        lineTo(t1x, t1y)
        // arcメソッドを呼び出す
        arc(cx, cy, radius, startArcAngle, endArcAngle, counterclockwiseArc)
    }

    /**
     * 3次ベジェ曲線を現在のパスに追加します。
     * @param cp1x 制御点1のX座標
     * @param cp1y 制御点1のY座標
     * @param cp2x 制御点2のX座標
     * @param cp2y 制御点2のY座標
     * @param x 終点のX座標
     * @param y 終点のY座標
     */
    fun bezierCurveTo(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float, x: Float, y: Float) {
        if (currentPath.isEmpty()) {
            // パスが空の場合は何もしない。
            return
        }

        val p0x = currentPath.last().first
        val p0y = currentPath.last().second

        // ベジェ曲線を多数の短い線分に分割して近似する
        val segments = 20 // 分割数。必要に応じて調整

        for (i in 1..segments) {
            val t = i.toFloat() / segments

            // Bézier curve equation
            // B(t) = (1-t)^3 * P0 + 3 * (1-t)^2 * t * P1 + 3 * (1-t) * t^2 * P2 + t^3 * P3
            val invT = 1 - t
            val invT2 = invT * invT
            val invT3 = invT2 * invT
            val t2 = t * t
            val t3 = t2 * t

            val pTx = invT3 * p0x + 3 * invT2 * t * cp1x + 3 * invT * t2 * cp2x + t3 * x
            val pTy = invT3 * p0y + 3 * invT2 * t * cp1y + 3 * invT * t2 * cp2y + t3 * y

            lineTo(pTx, pTy)
        }
    }
}
