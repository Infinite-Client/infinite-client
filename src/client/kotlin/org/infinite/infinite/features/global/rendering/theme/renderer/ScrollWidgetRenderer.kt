// org.infinite.infinite.features.global.rendering.theme.renderer.ScrollWidgetRenderer.kt
package org.infinite.infinite.features.global.rendering.theme.renderer

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractScrollArea
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha

class ScrollWidgetRenderer : WidgetRenderer<AbstractScrollArea> {
    override fun render(widget: AbstractScrollArea, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // 注: AbstractScrollArea 自体の render はバニラに任せ、
        // Mixinからは renderScrollbar だけをここに飛ばす形にします。
    }

    fun renderScrollbar(widget: AbstractScrollArea, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!widget.scrollbarVisible()) return

        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val g2d = Graphics2DRenderer(guiGraphics)

        val barX = widget.scrollBarX()
        val barY = widget.scrollBarY()
        val barHeight = widget.scrollerHeight()
        val barWidth = 4f
        val xCentered = barX + 1f // 6px幅の中央付近に配置

        // 1. スクロールバーの背景（レール）
        // alpha 0.5f で背景に馴染ませる
        theme.renderBackGround(barX.toFloat(), widget.y.toFloat(), 6f, widget.height.toFloat(), g2d, 0.3f)

        // 2. スクロールバー本体（つまみ）の色決定
        // isOverScrollbar は AbstractScrollArea の protected メソッドなので、
        // アクセサ経由で取得する必要があります。
        val isHovered = widget.isOverScrollbar(mouseX.toDouble(), mouseY.toDouble())

        val barColor = if (isHovered) {
            colorScheme.accentColor.alpha(255)
        } else {
            colorScheme.accentColor.alpha(160)
        }

        // 3. 描画
        g2d.fillStyle = barColor
        // 角丸にするとよりモダンになります
        g2d.fillRoundedRect(xCentered, barY.toFloat(), barWidth, barHeight.toFloat(), 2f)

        g2d.flush()
    }
}
