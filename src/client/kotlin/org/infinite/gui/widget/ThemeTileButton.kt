package org.infinite.gui.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.text.Text
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
) : ClickableWidget(x, y, width, height, Text.literal(theme.name)) {
    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val selected = isSelected()
        val borderColor = if (selected) theme.colors.primaryColor else theme.colors.secondaryColor
        val background = theme.colors.backgroundColor

        context.drawBorder(x, y, width, height, borderColor)
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, background)

        // Title
        val textX = x + 8
        val textY = y + 6
        context.drawTextWithShadow(textRenderer, Text.literal(theme.name), textX, textY, theme.colors.foregroundColor)

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
        click: Click,
        doubled: Boolean,
    ): Boolean {
        if (!isMouseOver(click.x, click.y)) return false
        playDownSound(MinecraftClient.getInstance().soundManager)
        onSelect()
        return true
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, this.message)
    }
}
