package org.infinite.libs.graphics.graphics2d.minecraft

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import org.infinite.libs.graphics.graphics2d.elements.ColoredDoubleRectangleRenderState
import org.joml.Matrix3x2f

fun GuiGraphics.fill(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    color: Int,
) {
    val renderPipeline = RenderPipelines.GUI
    val textureSetup = TextureSetup.noTexture()
    this.guiRenderState.submitGuiElement(
        ColoredDoubleRectangleRenderState(
            renderPipeline,
            textureSetup,
            Matrix3x2f(this.pose()),
            x,
            y,
            width,
            height,
            color,
            color,
            this.scissorStack.peek(),
        ),
    )
}
