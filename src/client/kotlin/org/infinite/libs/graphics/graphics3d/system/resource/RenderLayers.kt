package org.infinite.libs.graphics.graphics3d.system.resource

import net.minecraft.client.renderer.rendertype.LayeringTransform
import net.minecraft.client.renderer.rendertype.OutputTarget
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object RenderLayers {
    val lines: RenderType = RenderType.create(
        "infinite:lines",
        RenderSetup.builder(ShaderPipelines.depthTestLines)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup(),
    )

    val espLines: RenderType = RenderType.create(
        "infinite:esp_lines",
        RenderSetup.builder(ShaderPipelines.espLines)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup(),
    )

    /**
     * Similar to [RenderType.getDebugQuads], but with culling enabled.
     */
    val quads: RenderType = RenderType.create(
        "infinite:quads",
        RenderSetup.builder(ShaderPipelines.quads).sortOnUpload()
            .createRenderSetup(),
    )

    /**
     * Similar to [RenderType.getDebugQuads], but with culling enabled
     * and no depth test.
     */
    val espQuads: RenderType = RenderType.create(
        "infinite:esp_quads",
        RenderSetup.builder(ShaderPipelines.espQuads)
            .sortOnUpload().createRenderSetup(),
    )

    /**
     * Similar to [RenderType.getDebugQuads], but with no depth test.
     */
    val espQuadsNoCulling: RenderType = RenderType.create(
        "infinite:esp_quads_no_culling",
        RenderSetup.builder(ShaderPipelines.espQuadsNoCulling)
            .sortOnUpload().useLightmap().createRenderSetup(),
    )

    fun quads(depthTest: Boolean): RenderType = if (depthTest) quads else espQuads

    fun lines(depthTest: Boolean): RenderType = if (depthTest) lines else espLines
}
