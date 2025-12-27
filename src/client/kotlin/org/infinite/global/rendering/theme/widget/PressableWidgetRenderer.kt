package org.infinite.global.rendering.theme.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.CheckboxWidget
import net.minecraft.client.gui.widget.LockButtonWidget
import net.minecraft.client.gui.widget.PageTurnWidget
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.gui.widget.TextIconButtonWidget
import net.minecraft.client.gui.widget.TexturedButtonWidget
import org.infinite.InfiniteClient
import org.infinite.gui.theme.ThemeColors
import org.infinite.libs.graphics.Graphics2D
import org.infinite.utils.rendering.transparent

class PressableWidgetRenderer(
    val widget: PressableWidget,
) {
    fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, MinecraftClient.getInstance().renderTickCounter)
        val x = widget.x
        val y = widget.y
        val width = widget.width
        val height = widget.height
        val active = widget.active
        val hovered = widget.isHovered
        val colors: ThemeColors = InfiniteClient.currentColors()
        val alpha = widget.alpha
        val transparent = 255 * alpha
        // --- (背景とボーダーの描画ロジックは変更なし) ---
        val borderColor = colors.primaryColor.transparent(transparent)
        val textColor = colors.foregroundColor.transparent(transparent)
        val backgroundColor =
            when {
                hovered -> {
                    colors.primaryColor
                }

                active -> {
                    colors.backgroundColor
                }

                else -> {
                    colors.secondaryColor
                }
            }.transparent(transparent)

        graphics2D.fill(x, y, width, height, backgroundColor)
        val borderWidth = 1
        graphics2D.drawBorder(x, y, width, height, borderColor, borderWidth)
        // -------------------------------------------------

        val shouldRenderText =
            when (widget) {
                is CheckboxWidget,
                is LockButtonWidget,
                is PageTurnWidget,
                is TextIconButtonWidget.IconOnly,
                is TexturedButtonWidget,
                -> false
                else -> true
            }

        if (shouldRenderText) {
            val textRenderer = MinecraftClient.getInstance().textRenderer
            val textX = x + width / 2
            val textY = y + (height - textRenderer.fontHeight) / 2
            context.drawCenteredTextWithShadow(textRenderer, widget.message, textX, textY, textColor)
        }
    }
}
