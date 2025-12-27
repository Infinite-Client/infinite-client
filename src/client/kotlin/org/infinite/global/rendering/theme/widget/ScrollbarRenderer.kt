package org.infinite.global.rendering.theme.widget

import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractScrollArea
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.gui.theme.ThemeColors
import org.infinite.utils.rendering.transparent

class ScrollbarRenderer(
    val widget: AbstractScrollArea,
) {
    fun renderScrollbar(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float, // delta is not used in the original drawScrollbar, but kept for consistency
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)
        val colors: ThemeColors = InfiniteClient.currentColors()

        if (widget.scrollbarVisible()) {
            val i = widget.scrollBarX()
            val j = widget.scrollerHeight()
            val k = widget.scrollBarY()

            // カスタムのスクロールバー背景の描画
            graphics2D.fill(
                i,
                widget.y,
                6, // width of the scrollbar background
                widget.height,
                colors.backgroundColor.transparent(128), // 半透明の背景色
            )

            // カスタムのスクロールバーサム（つまみ）の描画
            graphics2D.fill(
                i,
                k,
                6, // width of the scrollbar thumb
                j,
                colors.primaryColor, // プライマリカラーのつまみ
            )
            graphics2D.drawBorder(
                i,
                k,
                6,
                j,
                colors.foregroundColor, // つまみのボーダー
                1,
            )

            if (widget.isOverScrollbar(mouseX.toDouble(), mouseY.toDouble())) {
                context.requestCursor(if (widget.scrolling) CursorTypes.RESIZE_NS else CursorTypes.POINTING_HAND)
            }
        }
    }
}
