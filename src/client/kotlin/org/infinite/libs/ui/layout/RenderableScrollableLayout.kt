package org.infinite.libs.ui.layout

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.ScrollableLayout
import net.minecraft.client.gui.layouts.Layout

class RenderableScrollableLayout(
    minecraft: Minecraft,
    layout: Layout,
    i: Int,
) : ScrollableLayout(minecraft, layout, i), Renderable {
    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        visitWidgets { widget ->
            if (widget is Renderable) {
                widget.render(guiGraphics, i, j, f)
            }
        }
    }
}
