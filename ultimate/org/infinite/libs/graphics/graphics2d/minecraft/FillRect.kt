package org.infinite.libs.graphics.graphics2d.minecraft

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import org.infinite.libs.graphics.graphics2d.elements.ColoredRectangleRenderState
import org.joml.Matrix3x2f

fun GuiGraphics.fill(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Int,
) {
    this.fill(x, y, width, height, color, color, color, color)
}

fun GuiGraphics.fill(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color0: Int,
    color1: Int,
    color2: Int,
    color3: Int,
) {
    val renderPipeline = RenderPipelines.GUI
    val textureSetup = TextureSetup.noTexture()
    this.guiRenderState.submitGuiElement(
        ColoredRectangleRenderState(
            renderPipeline,
            textureSetup,
            Matrix3x2f(this.pose()),
            x,
            y,
            width,
            height,
            color0,
            color1,
            color2,
            color3,
            this.scissorStack.peek(),
        ),
    )
}
