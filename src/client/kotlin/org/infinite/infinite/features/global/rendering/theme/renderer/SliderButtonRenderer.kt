package org.infinite.infinite.features.global.rendering.theme.renderer

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha

class SliderButtonRenderer : WidgetRenderer<AbstractSliderButton> {
    override fun render(widget: AbstractSliderButton, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val g2d = Graphics2DRenderer(guiGraphics)

        // 1. 全体背景の描画
        WidgetRenderUtils.renderCustomBackground(widget, g2d)

        // 2. ハンドル（ノブ）の描画
        val handleWidth = 8f
        // widget.value は 0.0 ~ 1.0 の進捗
        val handleX = (widget.x + (widget.value * (widget.width - handleWidth))).toFloat()
        val alphaInt = (widget.alpha * 255).toInt()

        val handleColor = if (widget.isHoveredOrFocused) {
            colorScheme.accentColor
        } else {
            colorScheme.secondaryColor
        }.alpha(alphaInt)

        g2d.fillStyle = handleColor

        // ハンドルを少し角丸に、かつボタンの高さに合わせる
        // ボタンより少しだけ内側に収める（上下1px空ける）とモダンに見えます
        val padding = 1f
        g2d.fillRoundedRect(
            handleX,
            widget.y + padding,
            handleWidth,
            widget.height - padding * 2,
            2f,
        )

        // 3. テキストの描画
        g2d.textStyle.apply {
            font = "infinite_regular"
            shadow = false
        }
        g2d.fillStyle = colorScheme.foregroundColor.alpha(alphaInt)
        g2d.textCentered(
            widget.message.string,
            widget.x + widget.width / 2f,
            widget.y + widget.height / 2f,
        )

        g2d.flush()
    }
}
