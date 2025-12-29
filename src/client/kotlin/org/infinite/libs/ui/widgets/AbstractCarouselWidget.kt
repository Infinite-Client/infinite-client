package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.libs.ui.screen.AbstractCarouselScreen

abstract class AbstractCarouselWidget<T>(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val data: T,
    val parent: AbstractCarouselScreen<T>,
    val thisIndex: Int,
    title: Component,
) : AbstractWidget(x, y, width, height, title) {

    protected val spawnTime = System.currentTimeMillis()
    protected val animationDuration = 250L

    /**
     * カスタム描画ロジックをここに記述します。
     */
    abstract fun renderCustom(graphics2D: AbstractCarouselScreen.WidgetGraphics2D): AbstractCarouselScreen.WidgetGraphics2D

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }

    final override fun renderWidget(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
    }

    override fun onClick(mouseButtonEvent: MouseButtonEvent, bl: Boolean) {
        parent.pageIndex = thisIndex
        onSelected(data)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return active && visible && mouseX >= x && mouseY >= y && mouseX < (x + width) && mouseY < (y + height)
    }

    /**
     * クリック（選択）された時の追加アクション
     */
    abstract fun onSelected(data: T)
}
