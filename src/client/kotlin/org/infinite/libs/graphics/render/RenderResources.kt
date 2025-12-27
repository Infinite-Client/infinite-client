package org.infinite.libs.graphics.render

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderSetup

object RenderResources {
    private val normalLines: RenderLayer =
        RenderLayer.of("infinite:lines", RenderSetup.builder(RenderPipelines.LINES).build())
    private val espLines: RenderLayer =
        RenderLayer.of("infinite:esp_lines", RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT).translucent().build())
    private val solidLayer: RenderLayer =
        RenderLayer.of("infinite:solid", RenderSetup.builder(RenderPipelines.GUI).translucent().build())

    fun renderLinedLayer(isOverDraw: Boolean): RenderLayer = if (isOverDraw) espLines else normalLines

    fun renderSolidLayer(@Suppress("UNUSED_PARAMETER") isOverDraw: Boolean): RenderLayer = solidLayer
}
