package org.infinite.gui.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D

class InfiniteButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    private val onPress: () -> Unit,
) : ClickableWidget(x, y, width, height, message) {
    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, MinecraftClient.getInstance().renderTickCounter)

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
        click: Click,
        doubled: Boolean,
    ): Boolean {
        if (!isMouseOver(click.x, click.y) || !active) return false
        playDownSound(MinecraftClient.getInstance().soundManager)
        onPress()
        return true
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }
}
