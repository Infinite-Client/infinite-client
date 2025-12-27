package org.infinite.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D

class TabButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
    private val onPress: () -> Unit,
) : AbstractWidget(x, y, width, height, message) {
    var isHighlighted: Boolean = false

    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)
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
