package org.infinite.infinite.features.global.rendering.theme.renderer

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha

class PlainButtonRenderer : WidgetRenderer<Button> {
    override fun render(widget: Button, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val g2d = Graphics2DRenderer(guiGraphics)
        val mc = Minecraft.getInstance()
        // 2. テキスト設定
        val text = widget.message.string
        val textWidth = mc.font.width(text)
        val innerWidth = widget.width - 8
        val alphaInt = (widget.alpha * 255).toInt()

        val textColor = if (widget.isActive) {
            colorScheme.foregroundColor
        } else {
            colorScheme.secondaryColor
        }.alpha(alphaInt)

        g2d.textStyle.apply {
            font = "infinite_regular"
            shadow = false // スクロール時はシャドウなしの方が見やすい
        }
        g2d.fillStyle = textColor

        val centerX = widget.x + widget.width / 2f
        val centerY = widget.y + widget.height / 2f

        // 3. スクロール描画判定
        if (textWidth > innerWidth && widget.isHoveredOrFocused) {
            val gap = 20f
            val speed = 40f
            val time = System.currentTimeMillis() / 1000f
            val loopRange = textWidth + gap
            val offset = (time * speed) % loopRange

            g2d.enableScissor(widget.x + 4, widget.y, widget.x + widget.width - 4, widget.y + widget.height)

            // 1回目の描画
            g2d.text(text, widget.x + 4f - offset, centerY)
            // 2回目の描画（ループ用）
            g2d.text(text, widget.x + 4f - offset + loopRange, centerY)

            g2d.disableScissor()
        } else {
            // 通常の中央揃え
            g2d.textCentered(text, centerX, centerY)
        }

        g2d.flush()
    }
}
