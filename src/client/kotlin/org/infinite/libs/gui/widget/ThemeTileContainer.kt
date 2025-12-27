package org.infinite.libs.gui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ThemeTileContainer(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val themeTileButtons: List<ThemeTileButton>,
    private val tileWidth: Int = 130, // Adjust as needed
    private val tileHeight: Int = 36, // Adjust as needed
    private val padding: Int = 5,
) : AbstractWidget(x, y, width, height, Component.empty()) {
    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        if (!visible) return

        val containerWidth = this.width

        val tilesPerRow =
            if (tileWidth + padding > containerWidth) {
                1
            } else {
                (containerWidth + padding) / (tileWidth + padding)
            }

        if (tilesPerRow == 0) return // Should not happen if containerWidth is positive

        // Calculate dynamic tile width to fill the row evenly
        val totalPaddingWidth = (tilesPerRow - 1).coerceAtLeast(0) * padding
        val actualTileWidth = (containerWidth - totalPaddingWidth) / tilesPerRow

        var currentX = this.x
        var currentY = this.y
        var tilesInCurrentRow = 0

        themeTileButtons.forEach { button ->
            if (tilesInCurrentRow >= tilesPerRow) {
                currentX = this.x
                currentY += tileHeight + padding
                tilesInCurrentRow = 0
            }

            button.x = currentX
            button.y = currentY
            button.width = actualTileWidth // Set dynamic width for the button
            button.render(context, mouseX, mouseY, delta)

            currentX += actualTileWidth + padding
            tilesInCurrentRow++
        }
    }

    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        if (!this.active || !this.visible) return false
        themeTileButtons.forEach { button ->
            if (button.mouseClicked(click, doubled)) {
                return true
            }
        }
        return super.mouseClicked(click, doubled)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        if (!this.active || !this.visible) return false
        themeTileButtons.forEach { button ->
            if (button.mouseReleased(click)) {
                return true
            }
        }
        return super.mouseReleased(click)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        themeTileButtons.forEach { button ->
            if (button.keyPressed(input)) {
                return true
            }
        }
        return super.keyPressed(input)
    }

    override fun charTyped(input: CharacterEvent): Boolean {
        themeTileButtons.forEach { button ->
            if (button.charTyped(input)) {
                return true
            }
        }
        return super.charTyped(input)
    }

    // Narration Builder for accessibility (optional, but good practice)
    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        themeTileButtons.forEach { it.updateNarration(builder) }
    }

    fun calculateHeight(containerWidth: Int): Int {
        if (themeTileButtons.isEmpty()) return 0

        val tilesPerRow =
            if (tileWidth + padding > containerWidth) {
                1 // If a single tile + padding exceeds container width, still show 1 tile
            } else {
                (containerWidth + padding) / (tileWidth + padding)
            }

        if (tilesPerRow == 0) return 0 // Avoid division by zero

        val numRows = (themeTileButtons.size + tilesPerRow - 1) / tilesPerRow
        return numRows * tileHeight + (numRows - 1).coerceAtLeast(0) * padding
    }
}
