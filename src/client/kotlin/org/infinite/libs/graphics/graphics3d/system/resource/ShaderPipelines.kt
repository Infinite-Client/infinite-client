package org.infinite.libs.graphics.graphics3d.system.resource

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

object ShaderPipelines {
    val foglessLinesSnippet: RenderPipeline.Snippet = RenderPipeline
        .builder(
            RenderPipelines.MATRICES_FOG_SNIPPET,
            RenderPipelines.GLOBALS_SNIPPET,
        )
        .withVertexShader(Identifier.parse("infinite:core/fogless_lines"))
        .withFragmentShader(Identifier.parse("infinite:core/fogless_lines"))
        .withBlend(BlendFunction.TRANSLUCENT).withCull(false)
        .withVertexFormat(
            DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
            VertexFormat.Mode.LINES,
        )
        .buildSnippet()

    /**
     * Similar to the LINES ShaderPipeline, but with no fog.
     */
    val depthTestLines: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(foglessLinesSnippet)
            .withLocation(
                Identifier.parse("infinite:pipeline/depth_test_lines"),
            )
            .build(),
    )

    val espLines: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(foglessLinesSnippet)
            .withLocation(Identifier.parse("infinite:pipeline/esp_lines"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build(),
    )

    val quads: RenderPipeline = RenderPipelines
        .register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.parse("infinite:pipeline/quads"))
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .build(),
        )

    val espQuads: RenderPipeline = RenderPipelines
        .register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.parse("infinite:pipeline/esp_quads"))
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build(),
        )

    val espQuadsNoCulling: RenderPipeline = RenderPipelines
        .register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.parse("infinite:pipeline/esp_quads_no_culling"))
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build(),
        )
}
