package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fillQuad

class QuadRenderer(private val guiGraphics: GuiGraphics) {

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
        val q = normalizeToCCW(x0, y0, x1, y1, x2, y2, x3, y3, col0, col1, col2, col3)
        guiGraphics.fillQuad(q.x0, q.y0, q.x1, q.y1, q.x2, q.y2, q.x3, q.y3, q.c0, q.c1, q.c2, q.c3)
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
        color: Int,
    ) {
        fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, color, color, color, color)
    }

    /**
     * 頂点と色のペアを保持するデータ構造
     */
    private data class NormalizedQuad(
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

    /**
     * 頂点の順序を反時計回り(CCW)に正規化し、対応する色も入れ替える
     */
    private fun normalizeToCCW(
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
}
