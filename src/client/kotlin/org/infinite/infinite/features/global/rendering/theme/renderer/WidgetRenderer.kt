package org.infinite.infinite.features.global.rendering.theme.renderer

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget

interface WidgetRenderer<T : AbstractWidget> {
    fun render(widget: T, guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float)
}
