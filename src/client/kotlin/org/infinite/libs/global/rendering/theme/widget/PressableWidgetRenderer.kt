package org.infinite.libs.global.rendering.theme.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Checkbox
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.gui.components.LockIconButton
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.screens.inventory.PageButton
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.gui.theme.ThemeColors
import org.infinite.utils.rendering.transparent

class PressableWidgetRenderer(
    val widget: AbstractButton,
) {
    fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)
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
                is Checkbox,
                is LockIconButton,
                is PageButton,
                is SpriteIconButton.CenteredIcon,
                is ImageButton,
                -> false

                else -> true
            }

        if (shouldRenderText) {
            val textRenderer = Minecraft.getInstance().font
            val textX = x + width / 2
            val textY = y + (height - textRenderer.lineHeight) / 2
            context.drawCenteredString(textRenderer, widget.message, textX, textY, textColor)
        }
    }
}
