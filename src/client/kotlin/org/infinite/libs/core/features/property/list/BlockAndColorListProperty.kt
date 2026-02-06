package org.infinite.libs.core.features.property.list

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.property.ListProperty
import org.infinite.libs.core.features.property.list.serializer.BlockAndColor
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.log.LogSystem
import org.infinite.libs.ui.widgets.SuggestInputWidget
import org.infinite.utils.alpha
import org.lwjgl.glfw.GLFW
import kotlin.jvm.optionals.getOrNull

class BlockAndColorListProperty(default: List<BlockAndColor>) : ListProperty<BlockAndColor>(default) {

    override fun convertElement(anyValue: Any): BlockAndColor? {
        if (anyValue is BlockAndColor) return anyValue
        if (anyValue is String) {
            val parts = anyValue.split("#")
            val blockId = parts[0].trim()
            val colorStr = parts.getOrNull(1) ?: "FFFFFFFF"
            val color = try {
                colorStr.toLong(16).toInt()
            } catch (_: Exception) {
                0xFFFFFFFF.toInt()
            }
            return BlockAndColor(blockId, color)
        }
        return null
    }

    override fun renderElement(graphics2D: Graphics2D, item: BlockAndColor, x: Int, y: Int, width: Int, height: Int) {
        val identifier = Identifier.tryParse(item.blockId)
        val block = identifier?.let { BuiltInRegistries.BLOCK.get(it).getOrNull()?.value() }
        val iconSize = height - 2f
        if (block != null) {
            graphics2D.blockCentered(block, x + height / 2f, y + height / 2f, iconSize)
        }

        graphics2D.textStyle.apply {
            font = "infinite_regular"
            size = height - 4f
            shadow = true
        }
        graphics2D.fillStyle = InfiniteClient.theme.colorScheme.foregroundColor
        graphics2D.text(item.blockId, x + height + 4, y + (height - 8) / 2)

        val boxSize = height - 6f
        graphics2D.fillStyle = item.color.alpha(255)
        graphics2D.fillRect(x + width - boxSize - 4f, y + (height - boxSize) / 2f, boxSize, boxSize)
    }

    override fun createInputWidget(
        x: Int,
        y: Int,
        width: Int,
        initialValue: BlockAndColor?,
        onComplete: (BlockAndColor?) -> Unit,
    ): AbstractWidget = BlockAndColorInputWidget(x, y, width, 20, initialValue, onComplete)

    private inner class BlockAndColorInputWidget(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        initialValue: BlockAndColor?,
        private val onComplete: (BlockAndColor?) -> Unit,
    ) : AbstractWidget(x, y, width, height, Component.empty()) {

        private val colorInputWidth = 75
        private val spacing = 2

        private val blockInput = SuggestInputWidget(
            x,
            y,
            width - colorInputWidth - spacing,
            initialValue?.blockId ?: "",
            suggestions = { BuiltInRegistries.BLOCK.keySet().map { it.toString() } },
            onComplete = {},
        )

        private val colorInput = EditBox(
            Minecraft.getInstance().font,
            x + width - colorInputWidth,
            y,
            colorInputWidth,
            height,
            Component.empty(),
        ).apply {
            val colorHex = initialValue?.color?.let { "%08X".format(it) } ?: "FFFFFFFF"
            value = "#$colorHex"
            setMaxLength(9)
        }

        init {
            blockInput.isFocused = true
            colorInput.isFocused = false
        }

        private fun submit() {
            val bId = blockInput.value
            val cStr = colorInput.value.removePrefix("#")
            val color = try {
                cStr.toLong(16).toInt()
            } catch (e: Exception) {
                LogSystem.error("Failed to parse color: $cStr, $e")
                0xFFFFFFFF.toInt()
            }
            onComplete(BlockAndColor(bId, color))
        }

        override fun keyPressed(keyEvent: KeyEvent): Boolean {
            val key = keyEvent.key

            // 左右端でフォーカス移動
            if (key == GLFW.GLFW_KEY_LEFT && currentInput().cursorPosition == 0) {
                swapFocus()
                return true
            }
            if (key == GLFW.GLFW_KEY_RIGHT && currentInput().cursorPosition >= currentInput().value.length) {
                swapFocus()
                return true
            }

            // Enter / Esc
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                submit()
                return true
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                onComplete(null)
                return true
            }

            return currentInput().keyPressed(keyEvent)
        }

        // 現在フォーカスされている入力欄を返す便利関数
        private fun currentInput() = if (blockInput.isFocused) blockInput else colorInput

        private fun swapFocus() {
            blockInput.isFocused = !blockInput.isFocused
            colorInput.isFocused = !colorInput.isFocused
        }

        override fun charTyped(characterEvent: CharacterEvent): Boolean = if (blockInput.isFocused) {
            blockInput.charTyped(characterEvent)
        } else {
            colorInput.charTyped(
                characterEvent,
            )
        }

        override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
            val blockClicked = blockInput.mouseClicked(mouseButtonEvent, bl)
            val colorClicked = colorInput.mouseClicked(mouseButtonEvent, bl)
            if (blockClicked) {
                blockInput.isFocused = true
                colorInput.isFocused = false
            } else if (colorClicked) {
                blockInput.isFocused = false
                colorInput.isFocused = true
            }
            return blockClicked || colorClicked || super.mouseClicked(mouseButtonEvent, bl)
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            blockInput.render(guiGraphics, mouseX, mouseY, partialTick)
            colorInput.render(guiGraphics, mouseX, mouseY, partialTick)
        }

        override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput)
        }
    }
}
