package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.minecraft.fillQuad
import org.infinite.libs.graphics.graphics2d.minecraft.fillRect

class RectRenderer(
    private val guiGraphics: GuiGraphics,
) {
    // --- Fill Logic ---

    fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        guiGraphics.fillRect(x, y, x + w, y + h, color)
    }

    fun fillRect(x: Float, y: Float, w: Float, h: Float, col0: Int, col1: Int, col2: Int, col3: Int) {
        guiGraphics.fillQuad(
            x, y, // 左上 (0)
            x + w, y, // 右上 (1)
            x + w, y + h, // 右下 (2)
            x, y + h, // 左下 (3)
            col0, col1, col2, col3,
        )
    }
}
