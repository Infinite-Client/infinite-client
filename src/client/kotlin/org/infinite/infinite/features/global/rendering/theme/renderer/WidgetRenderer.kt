package org.infinite.infinite.features.global.rendering.theme.renderer

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget

interface WidgetRenderer<T : AbstractWidget> {
    fun render(widget: T, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float)
}
