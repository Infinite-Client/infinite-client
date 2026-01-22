package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.property.SelectionProperty
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
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
            val theme = InfiniteClient.theme
            val colorScheme = theme.colorScheme

            val g2d = Graphics2DRenderer(guiGraphics)

            // --- 1. 背景描画 (Theme API を使用) ---
            val alpha = if (active) 1.0f else 0.5f
            theme.renderBackGround(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), g2d, alpha)

            // ホバー時のハイライト重ね掛け
            if (isHovered && active) {
                g2d.fillStyle = colorScheme.accentColor.mix(0x00000000, 0.8f) // 薄くアクセントカラーを乗せる
                g2d.fillRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
            }

            // --- 2. 枠線描画 (Graphics2D Stroke API) ---
            g2d.strokeStyle.width = 1.0f
            g2d.strokeStyle.color = if (isHovered && active) colorScheme.accentColor else colorScheme.secondaryColor
            g2d.strokeRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

            // --- 3. テキスト描画 ---
            val displayText = property.propertyString(property.value)
            g2d.textStyle.size = 10f // Minecraftデフォルトフォントに近いサイズに調整
            g2d.textStyle.font = "infinite_regular"
            g2d.fillStyle = if (active) colorScheme.foregroundColor else colorScheme.secondaryColor

            // 中央揃え
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
        // ラベルなどの描画
        super.renderWidget(guiGraphics, i, j, f)
        // サイクルボタン（内部で Graphics2D を使用）の描画
        cycleButton.render(guiGraphics, i, j, f)
    }
}
