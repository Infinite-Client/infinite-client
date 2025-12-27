package org.infinite.libs.graphics.graphics2d.minecraft

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import org.infinite.libs.graphics.graphics2d.elements.ColoredQuadRenderState
import org.joml.Matrix3x2f

/**
 * 4つの頂点とそれぞれの色を指定して四角形を描画します。
 */
fun GuiGraphics.fillQuad(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    x3: Float,
    y3: Float,
    col0: Int,
    col1: Int,
    col2: Int,
    col3: Int,
) {
    val renderPipeline = RenderPipelines.GUI
    val textureSetup = TextureSetup.noTexture()

    this.guiRenderState.submitGuiElement(
        ColoredQuadRenderState(
            renderPipeline,
            textureSetup,
            Matrix3x2f(this.pose()),
            x0,
            y0,
            x1,
            y1,
            x2,
            y2,
            x3,
            y3,
            col0,
            col1,
            col2,
            col3,
            this.scissorStack.peek(),
        ),
    )
}
