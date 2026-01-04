package org.infinite.global.rendering.theme.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.cursor.StandardCursors
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.OrderedText
import net.minecraft.util.Util
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import java.util.Objects
import kotlin.math.min

class TextFieldWidgetRenderer(
    val widget: TextFieldWidget,
) {
    private val client: MinecraftClient = MinecraftClient.getInstance()
    private val textRenderer: TextRenderer = client.textRenderer

    fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        if (widget.isVisible) {
            val graphics2D = Graphics2D(context)
            val colors = InfiniteClient.currentColors()
            val backgroundColor = colors.backgroundColor
            val primaryColor = colors.primaryColor
            val foregroundColor = colors.foregroundColor
            val secondaryColor = colors.secondaryColor
            val infoColor = colors.infoColor // color used for suggestion text

            // Draw background/border with subtle hover/focus emphasis
            if (widget.drawsBackground()) {
                val hovered = widget.isHovered
                val focused = widget.isFocused
                val baseAlpha =
                    if (focused) {
                        210
                    } else if (hovered) {
                        180
                    } else {
                        150
                    }
                val borderAlpha =
                    if (focused) {
                        255
                    } else if (hovered) {
                        220
                    } else {
                        180
                    }
                graphics2D.fill(
                    widget.x,
                    widget.y,
                    widget.getWidth(),
                    widget.getHeight(),
                    withAlpha(backgroundColor, baseAlpha),
                )
                graphics2D.drawBorder(
                    widget.x,
                    widget.y,
                    widget.width,
                    widget.height,
                    withAlpha(primaryColor, borderAlpha),
                    1,
                )
            }

            // Text styling swaps when field is disabled
            val textColor: Int = if (widget.isEditable) foregroundColor else secondaryColor

            val startCursorIndexInScreenString: Int = widget.selectionStart - widget.firstCharacterIndex

            val screenVisibleText: String =
                textRenderer.trimToWidth(
                    widget.text.substring(widget.firstCharacterIndex),
                    widget.innerWidth,
                )

            val isSelectionStartVisible =
                startCursorIndexInScreenString >= 0 && startCursorIndexInScreenString <= screenVisibleText.length

            val shouldDrawBlinkingCursor =
                widget.isFocused && (Util.getMeasuringTimeMs() - widget.lastSwitchFocusTime) / 300L % 2L == 0L && isSelectionStartVisible

            val endCursorIndexInScreenString =
                MathHelper.clamp(widget.selectionEnd - widget.firstCharacterIndex, 0, screenVisibleText.length)

            var textRenderX: Int = widget.textX

            if (!screenVisibleText.isEmpty()) {
                val textBeforeCursor =
                    if (isSelectionStartVisible) screenVisibleText.take(startCursorIndexInScreenString) else screenVisibleText
                val orderedTextBeforeCursor: OrderedText? = widget.format(textBeforeCursor, widget.firstCharacterIndex)

                context.drawText(
                    textRenderer,
                    orderedTextBeforeCursor,
                    textRenderX,
                    widget.textY,
                    textColor,
                    widget.textShadow,
                )
                textRenderX += textRenderer.getWidth(orderedTextBeforeCursor) + 1
            }

            val isCursorAtEndOrMaximized =
                widget.selectionStart < widget.text.length || widget.text.length >= widget.maxLength

            var cursorRenderX = textRenderX

            if (!isSelectionStartVisible) {
                cursorRenderX = if (startCursorIndexInScreenString > 0) widget.textX + widget.width else widget.textX
            } else if (isCursorAtEndOrMaximized) {
                cursorRenderX = textRenderX - 1
                --textRenderX
            }

            if (!screenVisibleText.isEmpty() && isSelectionStartVisible && startCursorIndexInScreenString < screenVisibleText.length) {
                context.drawText(
                    textRenderer,
                    widget.format(screenVisibleText.substring(startCursorIndexInScreenString), widget.selectionStart),
                    textRenderX,
                    widget.textY,
                    textColor,
                    widget.textShadow,
                )
            }

            if (widget.placeholder != null && screenVisibleText.isEmpty() && !widget.isFocused) {
                // Show placeholder when empty/unfocused
                context.drawTextWithShadow(
                    textRenderer,
                    widget.placeholder,
                    textRenderX,
                    widget.textY,
                    secondaryColor,
                )
            }

            if (!isCursorAtEndOrMaximized && widget.suggestion != null) {
                // Render inline suggestion
                context.drawText(
                    textRenderer,
                    widget.suggestion,
                    cursorRenderX - 1,
                    widget.textY,
                    infoColor,
                    widget.textShadow,
                )
            }

            if (endCursorIndexInScreenString != startCursorIndexInScreenString) {
                val endSelectionX: Int =
                    widget.textX + textRenderer.getWidth(screenVisibleText.take(endCursorIndexInScreenString))

                val selectionStartXClamped = min(cursorRenderX, widget.x + widget.width)
                val selectionEndXClamped = min(endSelectionX - 1, widget.x + widget.width)

                val selectionYStart: Int = widget.textY - 1
                Objects.requireNonNull<TextRenderer?>(textRenderer)
                val selectionYEnd: Int = selectionYStart + 1 + 9

                context.drawSelection(selectionStartXClamped, selectionYStart, selectionEndXClamped, selectionYEnd, false)
            }

            if (shouldDrawBlinkingCursor) {
                if (isCursorAtEndOrMaximized) {
                    // Filled caret when cursor sits at end
                    val caretYStart: Int = widget.textY - 1
                    val caretXEnd = cursorRenderX + 1
                    Objects.requireNonNull<TextRenderer?>(textRenderer)
                    val caretYEnd: Int = caretYStart + 1 + 9

                    context.fill(
                        cursorRenderX,
                        caretYStart,
                        caretXEnd,
                        caretYEnd,
                        foregroundColor,
                    )
                } else {
                    // Underscore caret when mid-string
                    context.drawText(textRenderer, "_", cursorRenderX, widget.textY, foregroundColor, widget.textShadow)
                }
            }

            if (widget.isHovered) {
                context.setCursor(if (widget.isEditable) StandardCursors.IBEAM else StandardCursors.NOT_ALLOWED)
            }
        }
    }

    private fun withAlpha(
        color: Int,
        alpha: Int,
    ): Int {
        val clampedAlpha = alpha.coerceIn(0, 255)
        return (clampedAlpha shl 24) or (color and 0x00FFFFFF)
    }
}
