package org.infinite.infinite.features.global.rendering.theme.renderer

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import org.infinite.InfiniteClient
import org.infinite.infinite.features.global.rendering.theme.renderer.WidgetRenderUtils
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.Font
import org.infinite.utils.alpha

class EditBoxRenderer : WidgetRenderer<EditBox> {
    override fun render(widget: EditBox, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val g2d = Graphics2DRenderer(guiGraphics)
        val font = Font("infinite_regular")
        val fontName = "infinite_regular"

        // 1. 背景描画
        WidgetRenderUtils.renderCustomBackground(widget, g2d)

        // 2. テキスト計算
        val textColor = colorScheme.foregroundColor.alpha((widget.alpha * 255).toInt())
        val textX = widget.x + 4f
        val textY = widget.y + (widget.height - 8) / 2f

        // EditBoxの内部状態にアクセス（リフレクションやアクセサ経由が必要な場合があります）
        // ※Mixin側で公開したプロパティを使用
        val value = widget.value
        val displayPos = widget.displayPos
        val innerWidth = widget.innerWidth

        val visibleText = font.plainSubstrByWidth(value.substring(displayPos), innerWidth)
        val cursorOffset = widget.cursorPos - displayPos
        val highlightOffset = widget.highlightPos - displayPos

        g2d.enableScissor(widget.x + 2, widget.y + 2, widget.width - 2, widget.height - 2)

        // 3. テキスト描画
        g2d.textStyle.apply {
            this.font = fontName
            this.shadow = false
        }
        g2d.fillStyle = textColor
        g2d.text(visibleText, textX, textY)

        // 4. カーソル
        val showCursor = widget.isFocused && (System.currentTimeMillis() / 500) % 2 == 0L
        if (cursorOffset in 0..visibleText.length) {
            val cursorX = textX + font.width(visibleText.substring(0, cursorOffset))
            if (showCursor) {
                g2d.fillStyle = colorScheme.accentColor
                g2d.fillRect(cursorX, textY, 1f, 9f)
            }

            // 5. 選択範囲
            if (highlightOffset != cursorOffset) {
                val low = 0.coerceAtLeast(visibleText.length.coerceAtMost(cursorOffset.coerceAtMost(highlightOffset)))
                val high = 0.coerceAtLeast(visibleText.length.coerceAtMost(cursorOffset.coerceAtLeast(highlightOffset)))
                val startX = textX + font.width(visibleText.substring(0, low))
                val endX = textX + font.width(visibleText.substring(0, high))

                g2d.fillStyle = colorScheme.accentColor.alpha(100)
                g2d.fillRect(startX, textY, endX - startX, 9f)
            }
        }

        g2d.disableScissor()
        g2d.flush()
    }
}
