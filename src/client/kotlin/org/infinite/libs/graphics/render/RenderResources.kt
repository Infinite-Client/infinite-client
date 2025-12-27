package org.infinite.libs.graphics.render

import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object RenderResources {
    private val normalLines: RenderType =
        RenderType.create("infinite:lines", RenderSetup.builder(RenderPipelines.LINES).createRenderSetup())
    private val espLines: RenderType =
        RenderType.create("infinite:esp_lines", RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT).sortOnUpload().createRenderSetup())
    private val solidLayer: RenderType =
        RenderType.create("infinite:solid", RenderSetup.builder(RenderPipelines.GUI).sortOnUpload().createRenderSetup())

    fun renderLinedLayer(isOverDraw: Boolean): RenderType = if (isOverDraw) espLines else normalLines

    fun renderSolidLayer(
        @Suppress("UNUSED_PARAMETER") isOverDraw: Boolean,
    ): RenderType = solidLayer
}
