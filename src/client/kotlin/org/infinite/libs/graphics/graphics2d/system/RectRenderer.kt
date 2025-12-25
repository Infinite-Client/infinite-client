package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fill
import org.infinite.libs.graphics.graphics2d.minecraft.fillQuad

class RectRenderer(
    private val guiGraphics: GuiGraphics,
) {
    // --- Fill Logic ---

    fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        guiGraphics.fill(x, y, x + w, y + h, color)
    }

    fun fillRect(x: Float, y: Float, w: Float, h: Float, col0: Int, col1: Int, col2: Int, col3: Int) {
        guiGraphics.fillQuad(
            x, y, // 左上
            x + w, y, // 右上
            x + w, y + h, // 右下
            x, y + h, // 左下
            col0, col1, col2, col3,
        )
    }

    // --- Stroke Logic ---

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

    /** * 四隅の色を指定する枠線描画
     * 内部で「外側の座標」と「内側の座標」における色をサンプリングします。
     */
    fun strokeRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        col0: Int,
        col1: Int,
        col2: Int,
        col3: Int, // 左上, 右上, 右下, 左下の色
        strokeWidth: Float,
    ) {
        val hw = strokeWidth / 2f

        // 外側境界 (Outer)
        val ox0 = x - hw
        val oy0 = y - hw
        val ox1 = x + w + hw
        val oy1 = y - hw
        val ox2 = x + w + hw
        val oy2 = y + h + hw
        val ox3 = x - hw
        val oy3 = y + h + hw

        // 内側境界 (Inner)
        val ix0 = x + hw
        val iy0 = y + hw
        val ix1 = x + w - hw
        val iy1 = y + hw
        val ix2 = x + w - hw
        val iy2 = y + h - hw
        val ix3 = x + hw
        val iy3 = y + h - hw

        // 頂点ごとの色のサンプリング
        // 外側(ox, oy)は元の色をそのまま使うか、厳密には外側もサンプリングが必要ですが、
        // 通常 stroke は元の矩形範囲を基準にするため、内側(ix, iy)の色を計算します。

        val (inCol0, inCol1, inCol2, inCol3) = if (strokeWidth > 0f) {
            QuadRenderer.QuadColorSampler.sample(
                ix0, iy0, ix1, iy1, ix2, iy2, ix3, iy3,
                x, y, x + w, y, x + w, y + h, x, y + h,
                col0, col1, col2, col3,
            )
        } else {
            listOf(col0, col1, col2, col3)
        }

        // ※外側の色も同様にサンプリングする場合は、さらに ox, oy 用に sample を呼び出します。
        // ここでは「元の頂点色を外側の縁の色」として扱い、内側を補間します。
        val outCol0 = col0

        // すべての辺を 反時計回り (外1 -> 外2 -> 内2 -> 内1) で描画

        // 上辺 (左上 -> 右上)
        drawEdge(ox0, oy0, ox1, oy1, ix1, iy1, ix0, iy0, outCol0, col1, inCol1, inCol0)
        // 右辺 (右上 -> 右下)
        drawEdge(ox1, oy1, ox2, oy2, ix2, iy2, ix1, iy1, col1, col2, inCol2, inCol1)
        // 下辺 (右下 -> 左下)
        drawEdge(ox2, oy2, ox3, oy3, ix3, iy3, ix2, iy2, col2, col3, inCol3, inCol2)
        // 左辺 (左下 -> 左上)
        drawEdge(ox3, oy3, ox0, oy0, ix0, iy0, ix3, iy3, col3, outCol0, inCol0, inCol3)
    }

    private fun drawEdge(
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
    ) {
        guiGraphics.fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, c0, c1, c2, c3)
    }
}
