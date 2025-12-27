package org.infinite.global.rendering.theme.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.LoadingDotsWidget
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D // Import Graphics2D
import org.infinite.libs.gui.theme.ThemeColors

class LoadingWidgetRenderer(
    val widget: LoadingDotsWidget,
) {
    fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker) // Instantiate Graphics2D

        val x = widget.x
        val y = widget.y
        val width = widget.width
        val height = widget.height
        val hovered = widget.isHovered
        val colors: ThemeColors = InfiniteClient.currentColors()

        // テーマとホバー状態に基づいて色を決定
        var backgroundColor = colors.backgroundColor
        var borderColor = colors.primaryColor

        if (hovered) {
            backgroundColor = colors.primaryColor
        }

        // カスタム背景の描画
        graphics2D.fill(x, y, width, height, backgroundColor)

        // カスタムボーダーの描画
        val borderWidth = 1 // 枠線の太さ
        graphics2D.drawBorder(x, y, width, height, borderColor, borderWidth)
    }
}
