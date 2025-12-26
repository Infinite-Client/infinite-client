package org.infinite.libs.graphics

import net.minecraft.client.DeltaTracker
import org.infinite.libs.graphics.graphics2d.impls.QuadColorSampler
import org.infinite.libs.graphics.graphics2d.impls.lerpColorInTriangle
import org.infinite.libs.graphics.graphics2d.impls.normalizeToCCW
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.graphics.graphics2d.system.PointPair
import org.infinite.libs.graphics.graphics2d.system.PointPair.Companion.calculateForMiter
import org.infinite.libs.interfaces.MinecraftInterface
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * MDN CanvasRenderingContext2D API を Minecraft GuiGraphics 上に再現するクラス。
 * zIndex を排除し、呼び出し順（画家のアルゴリズム）に従って描画コマンドを保持します。
 */
class Graphics2D(
    deltaTracker: DeltaTracker,
) : MinecraftInterface() {
    val gameDelta: Float = deltaTracker.gameTimeDeltaTicks
    val realDelta: Float = deltaTracker.realtimeDeltaTicks
    val width: Int = client?.window?.guiScaledWidth ?: 200
    val height: Int = client?.window?.guiScaledHeight ?: 150
    var strokeStyle: StrokeStyle? = null
    var fillStyle: Int = 0xFFFFFFFF.toInt()

    // zIndexによるソートが不要なため、単純なFIFOキューに変更
    // 100は初期容量ではなく、最大容量の指定になるため、必要に応じて調整してください
    private val commandQueue = LinkedList<RenderCommand>()

    // パス描画のためのプロパティ
    private var currentPath: MutableList<Pair<Float, Float>> = mutableListOf()
    private var startPath: Pair<Float, Float>? = null

    // --- fillRect ---

    fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        commandQueue.add(RenderCommand.FillRect(x, y, width, height, fillStyle, fillStyle, fillStyle, fillStyle))
    }

    // --- fillQuad ---

    fun fillQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, fillStyle, fillStyle, fillStyle, fillStyle)
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
        // 頂点データと色のペアをリスト化
        val vertices = mutableListOf(
            Vertex(x0, y0, col0),
            Vertex(x1, y1, col1),
            Vertex(x2, y2, col2),
            Vertex(x3, y3, col3),
        )

        // 重心を計算
        val centerX = vertices.map { it.x }.average().toFloat()
        val centerY = vertices.map { it.y }.average().toFloat()

        // 重心からの角度でソート (時計回り)
        // Math.atan2(y, x) は反時計回りなので、マイナスを付けてソート
        vertices.sortBy { atan2((it.y - centerY).toDouble(), (it.x - centerX).toDouble()) }

        commandQueue.add(
            RenderCommand.FillQuad(
                vertices[0].x, vertices[0].y,
                vertices[1].x, vertices[1].y,
                vertices[2].x, vertices[2].y,
                vertices[3].x, vertices[3].y,
                vertices[0].color, vertices[1].color, vertices[2].color, vertices[3].color,
            ),
        )
    }

    // 内部用ヘルパー
    private data class Vertex(val x: Float, val y: Float, val color: Int)
    // --- fillTriangle ---

    fun fillTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        fillTriangle(x0, y0, x1, y1, x2, y2, fillStyle, fillStyle, fillStyle)
    }

    fun fillTriangle(
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
        // 外積 (Vector Cross Product) を利用して回転方向を判定
        // (x1-x0)*(y2-y0) - (y1-y0)*(x2-x0)
        val crossProduct = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0)

        // crossProduct > 0 なら反時計回りなので、頂点1と2を入れ替えて時計回りにする
        if (crossProduct > 0) {
            addFillTriangle(x0, y0, x2, y2, x1, y1, col0, col2, col1)
        } else {
            addFillTriangle(x0, y0, x1, y1, x2, y2, col0, col1, col2)
        }
    }

    private fun addFillTriangle(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        c0: Int,
        c1: Int,
        c2: Int,
    ) {
        commandQueue.add(RenderCommand.FillTriangle(x0, y0, x1, y1, x2, y2, c0, c1, c2))
    }

    // --- strokeRect ---
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

        // ここで guiGraphics.fillQuad の代わりに commandQueue.add(RenderCommand.FillQuad(...)) を使用
        commandQueue.add(RenderCommand.FillQuad(p1.ix, p1.iy, p1.ox, p1.oy, p2.ox, p2.oy, p2.ix, p2.iy, col0, col0, col1, col1))
        commandQueue.add(RenderCommand.FillQuad(p2.ix, p2.iy, p2.ox, p2.oy, p3.ox, p3.oy, p3.ix, p3.iy, col1, col1, col2, col2))
        commandQueue.add(RenderCommand.FillQuad(p3.ix, p3.iy, p3.ox, p3.oy, p4.ox, p4.oy, p4.ix, p4.iy, col2, col2, col3, col3))
        commandQueue.add(RenderCommand.FillQuad(p4.ix, p4.iy, p4.ox, p4.oy, p1.ox, p1.oy, p1.ix, p1.iy, col3, col3, col0, col0))
    }

    // --- strokeQuad ---
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

    // --- strokeTriangle ---
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
        val p0 = calculateForMiter(x0, y0, x2, y2, x1, y1, hw)
        val p1 = calculateForMiter(x1, y1, x0, y0, x2, y2, hw)
        val p2 = calculateForMiter(x2, y2, x1, y1, x0, y0, hw)

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

    // --- Path API ---

    fun beginPath() {
        currentPath.clear()
        startPath = null
    }

    fun moveTo(x: Float, y: Float) {
        currentPath.add(x to y)
        if (startPath == null) {
            startPath = x to y
        }
    }

    fun lineTo(x: Float, y: Float) {
        currentPath.add(x to y)
    }

    fun closePath() {
        startPath?.let {
            if (currentPath.lastOrNull() != it) {
                currentPath.add(it)
            }
        }
    }

    fun strokePath() {
        val style = strokeStyle ?: return
        val strokeWidth = style.width
        val color = style.color

        if (currentPath.size < 2) return

        // パスを線分に分解して描画
        for (i in 0 until currentPath.size - 1) {
            val p1 = currentPath[i]
            val p2 = currentPath[i + 1]

            // drawLine 関数を呼び出す代わりに、直接四角形を構築
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
                    color, color, color, color,
                ),
            )
        }
        // パス描画後にパスをクリア
        currentPath.clear()
        startPath = null
    }

    /**
     * 登録された順にコマンドを取り出します
     */
    fun commands(): List<RenderCommand> = commandQueue.toList()
}
