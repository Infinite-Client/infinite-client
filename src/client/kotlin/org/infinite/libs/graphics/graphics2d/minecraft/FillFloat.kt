package org.infinite.libs.graphics.graphics2d.minecraft

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import org.infinite.libs.graphics.graphics2d.elements.ColoredFloatRectangleRenderState
import org.joml.Matrix3x2f

fun GuiGraphics.fill(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Int,
) {
    val renderPipeline = RenderPipelines.GUI ?: return
    val textureSetup = TextureSetup.noTexture()
    this.guiRenderState.submitGuiElement(
        ColoredFloatRectangleRenderState(
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
