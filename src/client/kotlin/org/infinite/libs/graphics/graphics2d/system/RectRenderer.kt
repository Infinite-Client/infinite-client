package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fill

class RectRenderer(
    private val guiGraphics: GuiGraphics,
) {
    /**
     * 矩形の枠線を描画する (Canvas API 準拠の拡大幅)
     */
    fun strokeRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
        strokeWidth: Float,
    ) {
        val halfWidth = strokeWidth / 2f

        // Canvasの挙動に合わせて、境界線を中心に外側と内側に半分ずつ広げる
        val xMin = x - halfWidth
        val yMin = y - halfWidth
        val xMax = x + w + halfWidth
        val yMax = y + h + halfWidth

        // 上辺
        guiGraphics.fill(xMin, yMin, xMax, yMin + strokeWidth, color)
        // 下辺
        guiGraphics.fill(xMin, yMax - strokeWidth, xMax, yMax, color)
        // 左辺 (上下の角を重複させない場合は yMin + strokeWidth ～ yMax - strokeWidth に調整)
        guiGraphics.fill(xMin, yMin + strokeWidth, xMin + strokeWidth, yMax - strokeWidth, color)
        // 右辺
        guiGraphics.fill(xMax - strokeWidth, yMin + strokeWidth, xMax, yMax - strokeWidth, color)
    }

    // --- オーバーロード (x2, y2 形式ではなく x, y, w, h 形式を想定) ---

    fun strokeRect(
        x: Int, y: Int, w: Int, h: Int,
        color: Int, strokeWidth: Int
    ) = strokeRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, strokeWidth.toFloat())

    fun strokeRect(
        x: Double, y: Double, w: Double, h: Double,
        color: Int, strokeWidth: Double
    ) = strokeRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, strokeWidth.toFloat())

    /**
     * 矩形を塗りつぶす (fillRect は境界線に影響されず、指定範囲をそのまま塗る)
     */
    fun fillRect(
        x: Float, y: Float, w: Float, h: Float,
        color: Int
    ) = guiGraphics.fill(x, y, x + w, y + h, color)

    fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int) =
        fillRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color)

    fun fillRect(x: Double, y: Double, w: Double, h: Double, color: Int) =
        fillRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color)
}
