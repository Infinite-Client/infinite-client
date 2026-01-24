package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.ClickGuiPalette
import org.infinite.libs.core.features.property.SelectionProperty
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
import org.infinite.utils.mix

class SelectionPropertyWidget<T : Any>(
    x: Int,
    y: Int,
    width: Int,
    property: SelectionProperty<T>,
) : PropertyWidget<SelectionProperty<T>>(
    x,
    y,
    width,
    DEFAULT_WIDGET_HEIGHT * 2,
    property,
) {
    private class PropertyCycleButton<T : Any>(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        private val property: SelectionProperty<T>,
    ) : AbstractWidget(x, y, width, height, Component.empty()) {

        override fun onClick(mouseButtonEvent: MouseButtonEvent, bl: Boolean) {
            val button = mouseButtonEvent.button()
            when (button) {
                0 -> property.next()
                1 -> property.previous()
                else -> return
            }
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
            val g2d = Graphics2DRenderer(guiGraphics)

            val alpha = if (active) 1.0f else 0.5f
            val radius = (height * 0.35f).coerceAtLeast(6f)
            val baseColor = ClickGuiPalette.PANEL_ALT
            val fill = if (isHovered && active) baseColor.mix(ClickGuiPalette.HOVER, 0.2f) else baseColor
            g2d.fillStyle = fill.alpha((alpha * 255).toInt())
            g2d.fillRoundedRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), radius)

            val displayText = property.propertyString(property.value)
            g2d.textStyle.size = Minecraft.getInstance().font.lineHeight.toFloat()
            g2d.textStyle.font = "infinite_regular"
            val textColor = if (active) ClickGuiPalette.TEXT else ClickGuiPalette.MUTED
            g2d.fillStyle = textColor.alpha((alpha * 255).toInt())
            g2d.textCentered(displayText, x + width / 2f, y + height / 2f)
            g2d.flush()
        }

        override fun updateWidgetNarration(output: NarrationElementOutput) {
            defaultButtonNarrationText(output)
        }
    }

    private val cycleButton = PropertyCycleButton(x, y, 100, DEFAULT_WIDGET_HEIGHT, property)

    override fun children(): List<net.minecraft.client.gui.components.events.GuiEventListener> = listOf(cycleButton)

    override fun relocate() {
        super.relocate()
        val twoLineLimit = 256
        val padding = 4
        cycleButton.height = DEFAULT_WIDGET_HEIGHT
        cycleButton.width = property.minWidth.coerceAtLeast(64) + padding * 2

        if (width > twoLineLimit) {
            cycleButton.x = x + width - cycleButton.width
            cycleButton.y = y
        } else {
            cycleButton.x = x
            cycleButton.y = y + height - cycleButton.height
        }
    }

    override fun renderWidget(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        super.renderWidget(guiGraphics, i, j, f)
        cycleButton.render(guiGraphics, i, j, f)
    }
}
