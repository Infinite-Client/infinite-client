package org.infinite.libs.graphics.graphics3d.system.resource

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

object ShaderPipelines {
    private val translucentState = ColorTargetState(BlendFunction.TRANSLUCENT)
    private val noDepthTest = DepthStencilState(CompareOp.ALWAYS_PASS, false)
    private val lequalDepthTest = DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true)
    val foglessLinesSnippet: RenderPipeline.Snippet = RenderPipeline
        .builder(
            RenderPipelines.FOG_SNIPPET,
            RenderPipelines.GLOBALS_SNIPPET,
        )
        .withVertexShader(Identifier.parse("infinite:core/fogless_lines"))
        .withFragmentShader(Identifier.parse("infinite:core/fogless_lines"))
        // .withBlend(BlendFunction.TRANSLUCENT) から変更
        .withColorTargetState(translucentState)
        .withCull(false)
        .withVertexFormat(
            DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
            VertexFormat.Mode.LINES,
        )
        .buildSnippet()

    val depthTestLines: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(foglessLinesSnippet)
            .withLocation(Identifier.parse("infinite:pipeline/depth_test_lines"))
            // 必要に応じて明示的にセット
            .withDepthStencilState(lequalDepthTest)
            .build(),
    )

    val espLines: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(foglessLinesSnippet)
            .withLocation(Identifier.parse("infinite:pipeline/esp_lines"))
            .withDepthStencilState(noDepthTest)
            .build(),
    )

    val quads: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.parse("infinite:pipeline/quads"))
            .withCull(false)
            .withDepthStencilState(lequalDepthTest)
            .build(),
    )

    val espQuads: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.parse("infinite:pipeline/esp_quads"))
            .withCull(false)
            .withDepthStencilState(noDepthTest)
            .build(),
    )
    val espQuadsNoCulling: RenderPipeline = RenderPipelines
        .register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.parse("infinite:pipeline/esp_quads_no_culling"))
                .withCull(false)
                .withDepthStencilState(noDepthTest).build(),
        )
}
