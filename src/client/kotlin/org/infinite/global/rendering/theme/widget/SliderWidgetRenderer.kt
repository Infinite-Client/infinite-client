package org.infinite.global.rendering.theme.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.SliderWidget
import org.infinite.InfiniteClient
import org.infinite.gui.theme.ThemeColors
import org.infinite.libs.graphics.Graphics2D

class SliderWidgetRenderer(
    val widget: SliderWidget,
) {
    fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val minecraftClient = MinecraftClient.getInstance()
        val graphics2D = Graphics2D(context, minecraftClient.renderTickCounter)
        val x = widget.x
        val y = widget.y
        val width = widget.width
        val height = widget.height
        val active = widget.active
        val hovered = widget.isHovered
        val colors: ThemeColors = InfiniteClient.currentColors()

        // --- 1. カスタム背景とボーダーの描画 ---
        var backgroundColor = colors.backgroundColor
        val borderColor = colors.primaryColor

        if (hovered) {
            backgroundColor = colors.primaryColor
        }

        if (!active) {
            backgroundColor = colors.secondaryColor
        }

        // スライダーの背景全体を描画
        graphics2D.fill(x, y, width, height, backgroundColor)
        val borderWidth = 1
        graphics2D.drawBorder(x, y, width, height, borderColor, borderWidth)

        // --- 2. スライダーのつまみ（ハンドル）の描画 ---

        // 💡 ハンドルの幅はバニラのSliderWidgetに合わせて 8px を使用するのが一般的です。
        val handleWidth = 8

        // 💡 ハンドルのX座標を計算: スライダーの値 (this.value) に基づき、移動範囲 (width - handleWidth) 内で位置を決定
        val handleX = x + (widget.value * (width - handleWidth)).toInt()

        // ハンドルのカスタムカラーを設定（ここでは背景色と区別するために primaryColor を使用）
        val handleColor = colors.primaryColor

        // ハンドル部分を描画
        graphics2D.fill(handleX, y, handleWidth, height, handleColor)

        // ハンドルにもボーダーを描画することで視認性を高める
        graphics2D.drawBorder(handleX, y, handleWidth, height, colors.foregroundColor, 1)

        // --- 3. メッセージ（テキスト）の描画 ---

        // スライダーのメッセージ（値を含むテキスト）を描画します。
        // バニラの描画コード (this.drawScrollableText) の挙動を再現するため、
        // centeredText ではなく、通常は左端に少しオフセットをかけて描画します。

        val textColor = if (active) colors.foregroundColor else colors.secondaryColor
        val textRenderer = minecraftClient.textRenderer
        val textX = x + 2
        val textY = y + (height - textRenderer.fontHeight) / 2
        context.drawTextWithShadow(textRenderer, widget.message, textX, textY, textColor)
    }
}
