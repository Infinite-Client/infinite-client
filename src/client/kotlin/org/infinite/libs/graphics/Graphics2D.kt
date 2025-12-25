package org.infinite.libs.graphics

import net.minecraft.client.DeltaTracker
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.graphics.graphics2d.system.PointPair
import org.infinite.libs.interfaces.MinecraftInterface
import java.util.*
import kotlin.math.abs

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

    // --- strokeRect ---

    fun strokeRect(x: Float, y: Float, width: Float, height: Float) {
        val style = strokeStyle ?: return
        val strokeColor = style.color
        val strokeWidth = style.width

        val hw = strokeWidth / 2f
        // 外側と内側の矩形座標を計算
        val ox0 = x - hw
        val oy0 = y - hw
        val ox1 = x + width + hw
        val oy1 = y - hw
        val ox2 = x + width + hw
        val oy2 = y + height + hw
        val ox3 = x - hw
        val oy3 = y + height + hw

        val ix0 = x + hw
        val iy0 = y + hw
        val ix1 = x + width - hw
        val iy1 = y + hw
        val ix2 = x + width - hw
        val iy2 = y + height - hw
        val ix3 = x - hw
        val iy3 = y + height - hw

        // 上・右・下・左の4つの台形を描画
        commandQueue.add(RenderCommand.FillQuad(ox0, oy0, ox1, oy1, ix1, iy1, ix0, iy0, strokeColor, strokeColor, strokeColor, strokeColor))
        commandQueue.add(RenderCommand.FillQuad(ox1, oy1, ox2, oy2, ix2, iy2, ix1, iy1, strokeColor, strokeColor, strokeColor, strokeColor))
        commandQueue.add(RenderCommand.FillQuad(ox2, oy2, ox3, oy3, ix3, iy3, ix2, iy2, strokeColor, strokeColor, strokeColor, strokeColor))
        commandQueue.add(RenderCommand.FillQuad(ox3, oy3, ox0, oy0, ix0, iy0, ix3, iy3, strokeColor, strokeColor, strokeColor, strokeColor))
    }

    fun strokeRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        col0: Int,
        col1: Int,
        col2: Int,
        col3: Int,
    ) {
        val style = strokeStyle ?: return
        val strokeWidth = style.width
        val hw = strokeWidth / 2f

        // 各頂点の PointPair (Outer と Inner) を生成
        val p0 = PointPair(x - hw, y - hw, x + hw, y + hw) // 左上
        val p1 = PointPair(x + width + hw, y - hw, x + width - hw, y + hw) // 右上
        val p2 = PointPair(x + width + hw, y + height + hw, x + width - hw, y + height - hw) // 右下
        val p3 = PointPair(x - hw, y + height + hw, x + hw, y + height - hw) // 左下

        // 内側の色をサンプリング（元の長方形のグラデーションから抽出）
        val innerCols = if (strokeWidth > 0f) {
            listOf(
                GeometryHelper.lerpColorInQuad(p0.ix, p0.iy, x, y, x + width, y, x + width, y + height, x, y + height, col0, col1, col2, col3),
                GeometryHelper.lerpColorInQuad(p1.ix, p1.iy, x, y, x + width, y, x + width, y + height, x, y + height, col0, col1, col2, col3),
                GeometryHelper.lerpColorInQuad(p2.ix, p2.iy, x, y, x + width, y, x + width, y + height, x, y + height, col0, col1, col2, col3),
                GeometryHelper.lerpColorInQuad(p3.ix, p3.iy, x, y, x + width, y, x + width, y + height, x, y + height, col0, col1, col2, col3),
            )
        } else {
            listOf(col0, col1, col2, col3)
        }

        // 4つのエッジを描画
        // 上辺: p0 -> p1
        commandQueue.add(RenderCommand.FillQuad(p0.ox, p0.oy, p1.ox, p1.oy, p1.ix, p1.iy, p0.ix, p0.iy, col0, col1, innerCols[1], innerCols[0]))
        // 右辺: p1 -> p2
        commandQueue.add(RenderCommand.FillQuad(p1.ox, p1.oy, p2.ox, p2.oy, p2.ix, p2.iy, p1.ix, p1.iy, col1, col2, innerCols[2], innerCols[1]))
        // 下辺: p2 -> p3
        commandQueue.add(RenderCommand.FillQuad(p2.ox, p2.oy, p3.ox, p3.oy, p3.ix, p3.iy, p2.ix, p2.iy, col2, col3, innerCols[3], innerCols[2]))
        // 左辺: p3 -> p0
        commandQueue.add(RenderCommand.FillQuad(p3.ox, p3.oy, p0.ox, p0.oy, p0.ix, p0.iy, p3.ix, p3.iy, col3, col0, innerCols[0], innerCols[3]))
    }
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
        // GeometryHelper で正規化された頂点と色を取得
        val q = GeometryHelper.normalizeQuadToCCW(x0, y0, x1, y1, x2, y2, x3, y3, col0, col1, col2, col3)
        commandQueue.add(
            RenderCommand.FillQuad(
                q.x0, q.y0,
                q.x1, q.y1,
                q.x2, q.y2,
                q.x3, q.y3,
                q.c0, q.c1, q.c2, q.c3,
            ),
        )
    }

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
        // GeometryHelper で正規化された頂点と色を取得
        val t = GeometryHelper.normalizeTriangleToCCW(x0, y0, x1, y1, x2, y2, col0, col1, col2)
        commandQueue.add(RenderCommand.FillTriangle(t.x0, t.y0, t.x1, t.y1, t.x2, t.y2, t.c0, t.c1, t.c2))
    }
    // --- strokeQuad / strokeTriangle ---

    fun strokeQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val style = strokeStyle ?: return
        val color = style.color
        val strokeWidth = style.width

        val hw = strokeWidth / 2f

        // 各頂点に対して、前後の頂点情報を渡してオフセット計算
        val p0 = PointPair.calculateForMiter(x0, y0, x3, y3, x1, y1, hw)
        val p1 = PointPair.calculateForMiter(x1, y1, x0, y0, x2, y2, hw)
        val p2 = PointPair.calculateForMiter(x2, y2, x1, y1, x3, y3, hw)
        val p3 = PointPair.calculateForMiter(x3, y3, x2, y2, x0, y0, hw)

        // エッジ描画
        commandQueue.add(RenderCommand.FillQuad(p0.ox, p0.oy, p1.ox, p1.oy, p1.ix, p1.iy, p0.ix, p0.iy, color, color, color, color)) // p0-p1の辺
        commandQueue.add(RenderCommand.FillQuad(p1.ox, p1.oy, p2.ox, p2.oy, p2.ix, p2.iy, p1.ix, p1.iy, color, color, color, color)) // p1-p2の辺
        commandQueue.add(RenderCommand.FillQuad(p2.ox, p2.oy, p3.ox, p3.oy, p3.ix, p3.iy, p2.ix, p2.iy, color, color, color, color)) // p2-p3の辺
        commandQueue.add(RenderCommand.FillQuad(p3.ox, p3.oy, p0.ox, p0.oy, p0.ix, p0.iy, p3.ix, p3.iy, color, color, color, color)) // p3-p0の辺
    }

    fun strokeTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        val style = strokeStyle ?: return
        val color = style.color
        val strokeWidth = style.width

        val hw = strokeWidth / 2f

        // 重心は使わず、前後の頂点との関係からオフセットを計算
        // p0 に対しては、前(prev)が p2、次(next)が p1
        val p0 = PointPair.calculateForMiter(x0, y0, x2, y2, x1, y1, hw)
        val p1 = PointPair.calculateForMiter(x1, y1, x0, y0, x2, y2, hw)
        val p2 = PointPair.calculateForMiter(x2, y2, x1, y1, x0, y0, hw)

        // 辺の描画
        commandQueue.add(RenderCommand.FillQuad(p0.ox, p0.oy, p1.ox, p1.oy, p1.ix, p1.iy, p0.ix, p0.iy, color, color, color, color))
        commandQueue.add(RenderCommand.FillQuad(p1.ox, p1.oy, p2.ox, p2.oy, p2.ix, p2.iy, p1.ix, p1.iy, color, color, color, color))
        commandQueue.add(RenderCommand.FillQuad(p2.ox, p2.oy, p0.ox, p0.oy, p0.ix, p0.iy, p2.ix, p2.iy, color, color, color, color))
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
        val width = strokeStyle?.width ?: 1.0f
        val hw = width / 2f

        // 1. 各角のオフセット座標を計算
        val p0 = PointPair.calculateForMiter(x0, y0, x2, y2, x1, y1, hw)
        val p1 = PointPair.calculateForMiter(x1, y1, x0, y0, x2, y2, hw)
        val p2 = PointPair.calculateForMiter(x2, y2, x1, y1, x0, y0, hw)

        // 2. 内側の色を決定
        // strokeWidthが極端に太い場合(例: 2px以上)のみ、厳密な補間計算を行う
        val (inCol0, inCol1, inCol2) = if (width > 2.0f) {
            Triple(
                GeometryHelper.lerpColorInTriangle(p0.ix, p0.iy, x0, y0, x1, y1, x2, y2, col0, col1, col2),
                GeometryHelper.lerpColorInTriangle(p1.ix, p1.iy, x0, y0, x1, y1, x2, y2, col0, col1, col2),
                GeometryHelper.lerpColorInTriangle(p2.ix, p2.iy, x0, y0, x1, y1, x2, y2, col0, col1, col2),
            )
        } else {
            // 幅が狭い場合は、元の頂点色をそのまま使う（高速）
            Triple(col0, col1, col2)
        }

        // 3. 描画
        commandQueue.add(
            RenderCommand.FillQuad(
                x0 = p0.ox, y0 = p0.oy,
                x1 = p1.ox, y1 = p1.oy,
                x2 = p1.ix, y2 = p1.iy,
                x3 = p0.ix, y3 = p0.iy,
                col0 = col0, col1 = col1, col2 = inCol1, col3 = inCol0,
            ),
        )
        commandQueue.add(
            RenderCommand.FillQuad(
                x0 = p1.ox, y0 = p1.oy,
                x1 = p2.ox, y1 = p2.oy,
                x2 = p2.ix, y2 = p2.iy,
                x3 = p1.ix, y3 = p1.iy,
                col0 = col1, col1 = col2, col2 = inCol2, col3 = inCol1,
            ),
        )
        commandQueue.add(
            RenderCommand.FillQuad(
                x0 = p2.ox, y0 = p2.oy,
                x1 = p0.ox, y1 = p0.oy,
                x2 = p0.ix, y2 = p0.iy,
                x3 = p2.ix, y3 = p2.iy,
                col0 = col2, col1 = col0, col2 = inCol0, col3 = inCol2,
            ),
        )
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
        col0: Int,
        col1: Int,
        col2: Int,
        col3: Int,
    ) {
        val width = strokeStyle?.width ?: 1.0f

        // 1. 反時計回りに正規化
        val q = GeometryHelper.normalizeQuadToCCW(x0, y0, x1, y1, x2, y2, x3, y3, col0, col1, col2, col3)

        val hw = width / 2f

        // 2. 正規化された座標で計算
        val p0 = PointPair.calculateForMiter(q.x0, q.y0, q.x3, q.y3, q.x1, q.y1, hw)
        val p1 = PointPair.calculateForMiter(q.x1, q.y1, q.x0, q.y0, q.x2, q.y2, hw)
        val p2 = PointPair.calculateForMiter(q.x2, q.y2, q.x1, q.y1, q.x3, q.y3, hw)
        val p3 = PointPair.calculateForMiter(q.x3, q.y3, q.x2, q.y2, q.x0, q.y0, hw)

        // 3. 内側の色をサンプリング
        val innerCols = if (width > 2.0f) {
            listOf(
                GeometryHelper.lerpColorInQuad(p0.ix, p0.iy, q.x0, q.y0, q.x1, q.y1, q.x2, q.y2, q.x3, q.y3, q.c0, q.c1, q.c2, q.c3),
                GeometryHelper.lerpColorInQuad(p1.ix, p1.iy, q.x0, q.y0, q.x1, q.y1, q.x2, q.y2, q.x3, q.y3, q.c0, q.c1, q.c2, q.c3),
                GeometryHelper.lerpColorInQuad(p2.ix, p2.iy, q.x0, q.y0, q.x1, q.y1, q.x2, q.y2, q.x3, q.y3, q.c0, q.c1, q.c2, q.c3),
                GeometryHelper.lerpColorInQuad(p3.ix, p3.iy, q.x0, q.y0, q.x1, q.y1, q.x2, q.y2, q.x3, q.y3, q.c0, q.c1, q.c2, q.c3),
            )
        } else {
            listOf(q.c0, q.c1, q.c2, q.c3)
        }

        // 4. エッジ描画
        commandQueue.add(
            RenderCommand.FillQuad(
                x0 = p0.ox, y0 = p0.oy,
                x1 = p1.ox, y1 = p1.oy,
                x2 = p1.ix, y2 = p1.iy,
                x3 = p0.ix, y3 = p0.iy,
                col0 = q.c0, col1 = q.c1, col2 = innerCols[1], col3 = innerCols[0],
            ),
        )
        commandQueue.add(
            RenderCommand.FillQuad(
                x0 = p1.ox, y0 = p1.oy,
                x1 = p2.ox, y1 = p2.oy,
                x2 = p2.ix, y2 = p2.iy,
                x3 = p1.ix, y3 = p1.iy,
                col0 = q.c1, col1 = q.c2, col2 = innerCols[2], col3 = innerCols[1],
            ),
        )
        commandQueue.add(
            RenderCommand.FillQuad(
                x0 = p2.ox, y0 = p2.oy,
                x1 = p3.ox, y1 = p3.oy,
                x2 = p3.ix, y2 = p3.iy,
                x3 = p2.ix, y3 = p2.iy,
                col0 = q.c2, col1 = q.c3, col2 = innerCols[3], col3 = innerCols[2],
            ),
        )
        commandQueue.add(
            RenderCommand.FillQuad(
                x0 = p3.ox, y0 = p3.oy,
                x1 = p0.ox, y1 = p0.oy,
                x2 = p0.ix, y2 = p0.iy,
                x3 = p3.ix, y3 = p3.iy,
                col0 = q.c3, col1 = q.c0, col2 = innerCols[0], col3 = innerCols[3],
            ),
        )
    }

    /**
     * 登録された順にコマンドを取り出します
     */
    fun commands(): List<RenderCommand> = commandQueue.toList()

    private object GeometryHelper {
        data class Vertex(val x: Float, val y: Float, val color: Int)

        data class NormalizedQuad(
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

        data class NormalizedTriangle( // 追加
            val x0: Float,
            val y0: Float,
            val x1: Float,
            val y1: Float,
            val x2: Float,
            val y2: Float,
            val c0: Int,
            val c1: Int,
            val c2: Int,
        )

        fun normalizeQuadToCCW(
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

        fun normalizeTriangleToCCW(
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
            c0: Int,
            c1: Int,
            c2: Int,
        ): NormalizedTriangle { // Triple -> NormalizedTriangle に変更
            val crossProduct = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0)
            return if (crossProduct > 0) { // 反時計回りなので、頂点1と2を入れ替えて時計回りにする
                NormalizedTriangle(x0, y0, x2, y2, x1, y1, c0, c2, c1) // Triple -> NormalizedTriangle に変更
            } else { // 既に時計回り
                NormalizedTriangle(x0, y0, x1, y1, x2, y2, c0, c1, c2) // Triple -> NormalizedTriangle に変更
            }
        }

        fun lerpColorInTriangle(
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
            // 縮退した三角形（面積0）の場合は安全にc0を返す
            if (abs(denom) < 1e-6f) return c0

            val w0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / denom
            val w1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / denom
            val w2 = 1f - w0 - w1

            // 0.0〜1.0にクランプ（浮動小数点の誤差対策）
            val cw0 = w0.coerceIn(0f, 1f)
            val cw1 = w1.coerceIn(0f, 1f)
            val cw2 = w2.coerceIn(0f, 1f)

            // アルファ値を安全に抽出 (0xFFL と Long を使うことで符号付きIntのバグを回避)
            val a0 = (c0 shr 24 and 0xFF).toFloat()
            val r0 = (c0 shr 16 and 0xFF).toFloat()
            val g0 = (c0 shr 8 and 0xFF).toFloat()
            val b0 = (c0 and 0xFF).toFloat()

            val a1 = (c1 shr 24 and 0xFF).toFloat()
            val r1 = (c1 shr 16 and 0xFF).toFloat()
            val g1 = (c1 shr 8 and 0xFF).toFloat()
            val b1 = (c1 and 0xFF).toFloat()

            val a2 = (c2 shr 24 and 0xFF).toFloat()
            val r2 = (c2 shr 16 and 0xFF).toFloat()
            val g2 = (c2 shr 8 and 0xFF).toFloat()
            val b2 = (c2 and 0xFF).toFloat()

            val a = (a0 * cw0 + a1 * cw1 + a2 * cw2).toInt()
            val r = (r0 * cw0 + r1 * cw1 + r2 * cw2).toInt()
            val g = (g0 * cw0 + g1 * cw1 + g2 * cw2).toInt()
            val b = (b0 * cw0 + b1 * cw1 + b2 * cw2).toInt()

            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        fun lerpColorInQuad(
            px: Float,
            py: Float,
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
        ): Int {
            // 四角形を2つの三角形 (0,1,2) と (0,2,3) に分割して判定
            if (isPointInTriangle(px, py, x0, y0, x1, y1, x2, y2)) {
                return lerpColorInTriangle(px, py, x0, y0, x1, y1, x2, y2, c0, c1, c2)
            } else {
                return lerpColorInTriangle(px, py, x0, y0, x2, y2, x3, y3, c0, c2, c3)
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
    }
}
