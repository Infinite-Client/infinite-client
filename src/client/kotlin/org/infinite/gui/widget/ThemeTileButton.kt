package org.infinite.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.gui.theme.Theme
import org.infinite.utils.rendering.drawBorder

class ThemeTileButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val theme: Theme,
    private val isSelected: () -> Boolean,
    private val onSelect: () -> Unit,
) : AbstractWidget(x, y, width, height, Component.literal(theme.name)) {
    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val textRenderer = Minecraft.getInstance().font
        val selected = isHoveredOrFocused
        val borderColor = if (selected) theme.colors.primaryColor else theme.colors.secondaryColor
        val background = theme.colors.backgroundColor

        context.drawBorder(x, y, width, height, borderColor)
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, background)

        // Title
        val textX = x + 8
        val textY = y + 6
        context.drawString(textRenderer, Component.literal(theme.name), textX, textY, theme.colors.foregroundColor)

        // Color dots (squares) under the title
        val swatches =
            listOf(
                theme.colors.backgroundColor,
                theme.colors.primaryColor,
                theme.colors.secondaryColor,
                theme.colors.foregroundColor,
            )
        val swatchSize = 8
        val spacing = 4
        var sx = x + 8
        val sy = y + height - swatchSize - 6
        swatches.forEach { color ->
            context.fill(sx, sy, sx + swatchSize, sy + swatchSize, color)
            sx += swatchSize + spacing
        }
    }

    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        if (!isMouseOver(click.x, click.y)) return false
        playDownSound(Minecraft.getInstance().soundManager)
        onSelect()
        return true
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        builder.add(NarratedElementType.TITLE, this.message)
    }
}
