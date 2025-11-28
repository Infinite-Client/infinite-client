package org.infinite.gui.widget

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text

class ThemeTileContainer(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val themeTileButtons: List<ThemeTileButton>,
    private val tileWidth: Int = 130, // Adjust as needed
    private val tileHeight: Int = 36, // Adjust as needed
    private val padding: Int = 5,
) : ClickableWidget(x, y, width, height, Text.empty()) {
    override fun renderWidget(
        context: DrawContext,
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
        click: Click,
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

    override fun mouseReleased(click: Click): Boolean {
        if (!this.active || !this.visible) return false
        themeTileButtons.forEach { button ->
            if (button.mouseReleased(click)) {
                return true
            }
        }
        return super.mouseReleased(click)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        themeTileButtons.forEach { button ->
            if (button.keyPressed(input)) {
                return true
            }
        }
        return super.keyPressed(input)
    }

    override fun charTyped(input: CharInput): Boolean {
        themeTileButtons.forEach { button ->
            if (button.charTyped(input)) {
                return true
            }
        }
        return super.charTyped(input)
    }

    // Narration Builder for accessibility (optional, but good practice)
    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        themeTileButtons.forEach { it.appendNarrations(builder) }
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
