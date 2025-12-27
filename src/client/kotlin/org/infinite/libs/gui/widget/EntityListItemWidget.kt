package org.infinite.libs.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D

class EntityListItemWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int, // This height is for the individual item
    private val entityId: String,
    private val onRemove: (String) -> Unit,
) : AbstractWidget(x, y, width, height, Component.literal(entityId)) {
    private val padding = 8
    private val iconSize = 16
    private val iconPadding = 2
    private val iconTotalWidth = iconSize + iconPadding
    private val removeButtonWidth = 20

    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)

        val textX = iconTotalWidth
        val textY = y + this.height / 2 - 4
        graphics2D.drawText(
            Component.literal(entityId),
            textX,
            textY,
            InfiniteClient
                .currentColors()
                .foregroundColor,
            true, // shadow = true
        )

        val removeButtonX = x + width - padding - removeButtonWidth
        val removeButtonY = y
        val isRemoveButtonHovered =
            mouseX >= removeButtonX &&
                mouseX < removeButtonX + removeButtonWidth &&
                mouseY >= removeButtonY &&
                mouseY < removeButtonY + this.height

        val baseColor =
            InfiniteClient
                .currentColors()
                .errorColor
        val hoverColor =
            InfiniteClient
                .currentColors()
                .errorColor
        val removeColor = if (isRemoveButtonHovered) hoverColor else baseColor

        context.fill(
            removeButtonX,
            removeButtonY,
            removeButtonX + removeButtonWidth,
            removeButtonY + this.height,
            removeColor,
        )
        graphics2D.drawText(
            "x",
            removeButtonX + removeButtonWidth / 2 - 3,
            removeButtonY + this.height / 2 - 4,
            InfiniteClient
                .currentColors()
                .foregroundColor,
            false, // shadow = false
        )
    }

    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        val removeButtonX = x + width - padding - removeButtonWidth
        val removeButtonY = y
        val mouseX = click.x
        val mouseY = click.y
        if (mouseX >= removeButtonX &&
            mouseX < removeButtonX + removeButtonWidth &&
            mouseY >= removeButtonY &&
            mouseY < removeButtonY + this.height
        ) {
            onRemove(entityId)
            return true
        }
        return super.mouseClicked(click, doubled)
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        builder.add(NarratedElementType.TITLE, Component.literal("Entity List Item: $entityId"))
    }
}
