package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.property.SelectionProperty
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
    DEFAULT_WIDGET_HEIGHT * 2, // 2行分確保
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
            // 左クリックなら次へ、右クリックなら前へ
            val button = mouseButtonEvent.button()
            when (button) {
                0 -> property.next()
                1 -> property.previous()
                else -> return
            }
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
            val colorScheme = InfiniteClient.theme.colorScheme
            val font = Minecraft.getInstance().font

            // --- 背景 ---
            val backgroundColor = if (isHovered) {
                colorScheme.secondaryColor.mix(colorScheme.accentColor, 0.3f)
            } else {
                colorScheme.backgroundColor
            }
            guiGraphics.fill(x, y, x + width, y + height, backgroundColor)

            // --- 枠線 ---
            val strokeColor = if (isHovered) colorScheme.accentColor else colorScheme.secondaryColor
            guiGraphics.fill(x, y, x + width, y + 1, strokeColor) // 上
            guiGraphics.fill(x, y + height - 1, x + width, y + height, strokeColor) // 下
            guiGraphics.fill(x, y, x + 1, y + height, strokeColor) // 左
            guiGraphics.fill(x + width - 1, y, x + width, y + height, strokeColor) // 右

            // --- テキスト描画 ---
            val displayText = property.propertyString(property.value)
            val textWidth = font.width(displayText)

            guiGraphics.drawString(
                font,
                displayText,
                x + (width - textWidth) / 2,
                y + (height - 8) / 2,
                if (active) colorScheme.foregroundColor else colorScheme.secondaryColor,
                false,
            )
        }

        override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)
    }

    // サイクルボタンのインスタンス作成 (初期位置は relocate で設定)
    private val cycleButton = PropertyCycleButton(x, y, 100, DEFAULT_WIDGET_HEIGHT, property)

    override fun children(): List<GuiEventListener> = listOf(cycleButton)

    override fun relocate() {
        super.relocate()
        val twoLineLimit = 256

        // ボタンのサイズと位置の決定
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
        // PropertyWidget のラベル描画
        super.renderWidget(guiGraphics, i, j, f)
        // サイクルボタンの描画
        cycleButton.render(guiGraphics, i, j, f)
    }
}
