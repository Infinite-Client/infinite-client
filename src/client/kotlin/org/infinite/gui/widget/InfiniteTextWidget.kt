package org.infinite.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import org.infinite.libs.graphics.Graphics2D

class InfiniteTextWidget(
    x: Int,
    width: Int,
    y: Int,
    height: Int,
    private val text: Component,
    private val color: Int, // The color is directly passed, so no theme colors here.
) : AbstractWidget(x, y, width, height, text) {
    private val padding = 2

    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)

        graphics2D.drawText(
            text,
            this.x + padding,
            this.y + padding,
            color, // This color is already dynamic, no change needed.
        )
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        builder.add(NarratedElementType.TITLE, text)
    }
}
