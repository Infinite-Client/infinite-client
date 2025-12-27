package org.infinite.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D

class InfiniteButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
    private val onPress: () -> Unit,
) : AbstractWidget(x, y, width, height, message) {
    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)

        val borderWidth = 1 // 1px border
        val colors = InfiniteClient.currentColors()

        // Outer background
        context.fill(x, y, x + width, y + height, colors.backgroundColor)
        // Animated border
        context.fill(
            x + borderWidth,
            y + borderWidth,
            x + width - borderWidth,
            y + height - borderWidth,
            colors.primaryColor,
        )
        // Inner background
        context.fill(
            x + borderWidth * 2,
            y + borderWidth * 2,
            x + width - borderWidth * 2,
            y + height - borderWidth * 2,
            colors.backgroundColor,
        )

        val textColor = if (isHovered) colors.primaryColor else colors.foregroundColor
        graphics2D.centeredText(
            message,
            x + width / 2,
            y + height / 2,
            textColor,
            true,
        )
    }

    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        if (!isMouseOver(click.x, click.y) || !active) return false
        playDownSound(Minecraft.getInstance().soundManager)
        onPress()
        return true
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        defaultButtonNarrationText(builder)
    }
}
