package org.infinite.utils

import net.minecraft.client.gui.components.AbstractWidget
import org.infinite.InfiniteClient.theme
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import kotlin.math.min

object WidgetRenderUtils {
    /**
     * Infiniteテーマに基づいた共通のウィジェット背景と枠線を描画します。
     */
    fun renderCustomBackground(widget: AbstractWidget, renderer: Graphics2DRenderer) {
        val theme = theme
        val colorScheme = theme.colorScheme

        val alpha = widget.getAlpha()
        val alphaInt = (alpha * 255).toInt()

        // 1. 背景描画 (無効時は少し透過させる等のロジック)
        val bgAlpha = if (widget.active) alpha else alpha / 2f
        val radius = (widget.height * 0.35f).coerceAtLeast(6f)
        val baseColor = colorScheme.surfaceColor.mix(colorScheme.backgroundColor, 0.5f)
        renderer.fillStyle = baseColor.alpha((255 * bgAlpha).toInt())
        renderer.fillRoundedRect(
            widget.x.toFloat(),
            widget.y.toFloat(),
            widget.width.toFloat(),
            widget.height.toFloat(),
            radius,
        )

        // 2. 状態に応じた枠線色の決定
        val strokeColor: Int = if (widget.isHoveredOrFocused) {
            colorScheme.accentColor.alpha(alphaInt)
        } else if (widget.active) {
            colorScheme.secondaryColor.alpha(alphaInt)
        } else {
            // 無効時
            colorScheme.backgroundColor.alpha(min(255, alphaInt * 2))
        }

        // 3. 枠線の描画
        renderer.strokeStyle.width = 1f
        renderer.strokeStyle.color = strokeColor
        renderer.strokeRoundedRect(
            widget.x.toFloat(),
            widget.y.toFloat(),
            widget.width.toFloat(),
            widget.height.toFloat(),
            radius,
        )
    }
}
