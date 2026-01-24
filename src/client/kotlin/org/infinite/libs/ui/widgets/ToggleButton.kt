package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.ClickGuiPalette
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.mix

abstract class ToggleButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) : Button(
    x,
    y,
    width,
    height,
    Component.empty(),
    { button ->
        val tb = button as ToggleButton
        tb.value = !tb.value
    },
    DEFAULT_NARRATION,
) {
    protected abstract var value: Boolean

    fun render(graphics2D: Graphics2D) {
        val padding = 2f
        val barX = x + padding
        val barY = y + padding
        val barW = width - padding * 2f
        val barH = height - padding * 2f
        val knobSize = barH
        val knobX = if (value) barX + barW - knobSize else barX

        val baseBar = if (value) ClickGuiPalette.ACCENT_DARK else ClickGuiPalette.PANEL_ALT
        val barColor = if (isHovered) baseBar.mix(ClickGuiPalette.HOVER, 0.2f) else baseBar
        val knobColor = if (value) ClickGuiPalette.TEXT else ClickGuiPalette.MUTED
        val barRadius = barH / 2f
        val knobRadius = knobSize / 2f

        graphics2D.fillStyle = barColor
        graphics2D.fillRoundedRect(barX, barY, barW, barH, barRadius)

        graphics2D.fillStyle = knobColor
        graphics2D.fillRoundedRect(knobX, barY, knobSize, knobSize, knobRadius)
    }

    override fun renderContents(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)
        render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }
}

