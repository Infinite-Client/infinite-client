package org.infinite.gui.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D

class TabButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    private val onPress: () -> Unit,
) : ClickableWidget(x, y, width, height, message) {
    var isHighlighted: Boolean = false

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, MinecraftClient.getInstance().renderTickCounter)
        val colors = InfiniteClient.currentColors()
        val textColor = if (isHovered || isHighlighted) colors.primaryColor else colors.foregroundColor
        val backgroundColor = if (isHovered || isHighlighted) colors.backgroundColor else colors.secondaryColor
        graphics2D.fill(x, y, width, height, backgroundColor)
        graphics2D.drawBorder(x, y, width, height, colors.primaryColor)
        graphics2D.centeredText(
            message,
            x + width / 2,
            y + height / 2,
            textColor,
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
