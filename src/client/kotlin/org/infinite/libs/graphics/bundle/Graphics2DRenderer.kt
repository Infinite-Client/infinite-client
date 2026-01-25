package org.infinite.libs.graphics.bundle

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.RenderSystem2D

class Graphics2DRenderer(guiGraphics: GuiGraphics) : Graphics2D() {
    private val renderSystem2D = RenderSystem2D(guiGraphics)
    fun flush() {
        renderSystem2D.render(commands())
    }
}
