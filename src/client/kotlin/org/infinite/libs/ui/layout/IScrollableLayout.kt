package org.infinite.libs.ui.layout

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.ScrollableLayout
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.Layout
import net.minecraft.client.gui.navigation.ScreenRectangle

class IScrollableLayout(
    minecraft: Minecraft,
    layout: Layout,
    i: Int,
) : ScrollableLayout(minecraft, layout, i), Renderable, GuiEventListener {
    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        visitWidgets { widget ->
            if (widget is Renderable) {
                widget.render(guiGraphics, i, j, f)
            }
        }
    }

    private var focused = false
    override fun getRectangle(): ScreenRectangle =
        ScreenRectangle(x, y, width, height)

    override fun setFocused(bl: Boolean) {
        focused = bl
    }

    override fun isFocused(): Boolean = focused
}
