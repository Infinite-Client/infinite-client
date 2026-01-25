package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.property.SelectionProperty
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha

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
    // 内部ボタンクラス
    private class PropertyCycleButton<T : Any>(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        private val property: SelectionProperty<T>,
    ) : AbstractWidget(x, y, width, height, Component.empty()) {

        private val themeScheme get() = InfiniteClient.theme.colorScheme

        override fun onClick(mouseButtonEvent: MouseButtonEvent, bl: Boolean) {
            val button = mouseButtonEvent.button()
            when (button) {
                0 -> property.next()

                // 左クリック: 次へ
                1 -> property.previous()

                // 右クリック: 前へ
                else -> return
            }
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
            val g2d = Graphics2DRenderer(guiGraphics)
            val alphaMult = if (active) 1.0f else 0.5f
            val radius = (height / 2f).coerceAtLeast(4f)

            // --- 背景の描画 ---
            // surfaceColorをベースに、ホバー時はアクセントカラーを極薄く混ぜる
            var bg = themeScheme.surfaceColor
            if (isHovered && active) {
                bg = themeScheme.getHoverColor(bg)
            }

            // メイン背景
            g2d.fillStyle = bg.alpha((255 * alphaMult).toInt())
            g2d.fillRoundedRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), radius)

            // 外枠（アクセントカラーを薄く。アクティブ感を出す）
            g2d.strokeStyle.color = themeScheme.accentColor.alpha((40 * alphaMult).toInt())
            g2d.strokeStyle.width = 2f
            g2d.strokeRoundedRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), radius)

            // --- テキストの描画 ---
            val displayText = property.propertyString(property.value)
            g2d.textStyle.apply {
                size = Minecraft.getInstance().font.lineHeight.toFloat()
                font = "infinite_regular"
            }

            // ホバー時はテキストをアクセントカラーに寄せる
            val textColor = if (!active) {
                themeScheme.secondaryColor
            } else if (isHovered) {
                themeScheme.accentColor
            } else {
                themeScheme.foregroundColor
            }

            g2d.fillStyle = textColor.alpha((255 * alphaMult).toInt())
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
        val twoLineLimit = 220 // 少し厳しめに設定
        val padding = 8
        val font = Minecraft.getInstance().font

        // 選択肢の中で最大の幅を持つ文字列に合わせてボタン幅を調整
        val maxOptionWidth = property.propertyString(property.value).let { font.width(it) }
        cycleButton.height = (DEFAULT_WIDGET_HEIGHT * 0.9f).toInt()
        cycleButton.width = (maxOptionWidth + padding * 3).coerceIn(80, width / 2)

        if (width > twoLineLimit) {
            // 十分な幅があれば右側に配置
            cycleButton.x = x + width - cycleButton.width
            cycleButton.y = y + (DEFAULT_WIDGET_HEIGHT - cycleButton.height) / 2
        } else {
            // 幅が狭ければプロパティ名の下に配置
            cycleButton.x = x
            cycleButton.y = y + DEFAULT_WIDGET_HEIGHT + 2
        }
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // PropertyWidgetのラベル描画を呼び出す
        super.renderWidget(guiGraphics, mouseX, mouseY, delta)
        cycleButton.render(guiGraphics, mouseX, mouseY, delta)
    }
}
