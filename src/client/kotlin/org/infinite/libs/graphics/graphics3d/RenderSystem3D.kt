package org.infinite.libs.graphics.graphics3d

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.resource.GraphicsResourceAllocator
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.gizmos.Gizmos
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.graphics3d.structs.RenderCommand3D
import org.infinite.libs.graphics.graphics3d.system.QuadRenderer
import org.infinite.libs.graphics.graphics3d.system.TexturedRenderer
import org.infinite.libs.graphics.graphics3d.system.resource.RenderLayers
import org.infinite.libs.interfaces.MinecraftInterface
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Vector4f
import java.lang.foreign.ValueLayout
import java.util.*

@Suppress("unused")
class RenderSystem3D(
    private val graphicsResourceAllocator: GraphicsResourceAllocator,
    private val deltaTracker: DeltaTracker,
    private val renderBlockOutline: Boolean,
    private val camera: Camera,
    private val positionMatrix: Matrix4f,
    private val projectionMatrix: Matrix4f,
    private val frustumMatrix: Matrix4f,
    private val gpuBufferSlice: GpuBufferSlice,
    private val vector4f: Vector4f,
    private val bl2: Boolean,
) : MinecraftInterface() {

    private val bufferSource = minecraft.renderBuffers().bufferSource()
    private val quadRenderer = QuadRenderer(bufferSource)
    private val texturedRenderer = TexturedRenderer(bufferSource)

    /**
     * レンダリングスレッドから計算スレッドへ渡すための安全なスナップショット
     */
    data class RenderSnapshot(
        val posMatrix: Matrix4f, // Matrix4f(positionMatrix) でコピー済み
        val projMatrix: Matrix4f, // Matrix4f(projectionMatrix) でコピー済み
        val cameraPos: Vec3, // camera.position
        val partialTicks: Float, // deltaTracker.gameTimeDeltaTicks
        val scaledWidth: Int,
        val scaledHeight: Int,
        val isOutlineEnabled: Boolean, // renderBlockOutline
    )

    fun snapShot(): RenderSnapshot {
        val window = minecraft.window

        return RenderSnapshot(
            posMatrix = Matrix4f(positionMatrix),
            projMatrix = Matrix4f(projectionMatrix),
            cameraPos = Vec3(camera.position().x, camera.position().y, camera.position().z),
            partialTicks = deltaTracker.gameTimeDeltaTicks,
            scaledWidth = window.guiScaledWidth,
            scaledHeight = window.guiScaledHeight,
            isOutlineEnabled = renderBlockOutline,
        )
    }

    fun test() {
        val player = minecraft.player ?: return
        val start = player.position()
        val end = start.add(10.0, 10.0, 10.0)
        drawLine(start, end, 0xFFFF0000.toInt(), 2f, false)
    }

//    private val cameraPos: Vec3
//        get() = camera.position()

    fun drawLine(start: Vec3, end: Vec3, color: Int, lineWidth: Float, depthTest: Boolean = true) {
        val props = Gizmos.line(start, end, color, lineWidth)
        if (!depthTest) {
            props.setAlwaysOnTop()
        }
    }

    // --- Parser Direct Methods ---

    fun drawTriangleDirect(a: Vec3, b: Vec3, c: Vec3, color: Int, depthTest: Boolean) {
        drawTriangle(a, b, c, color, depthTest)
    }

    fun drawTriangleGradientDirect(a: Vec3, b: Vec3, c: Vec3, ca: Int, cb: Int, cc: Int, depthTest: Boolean) {
        val renderType = RenderLayers.quads(depthTest)
        quadRenderer.drawTriangle(renderType, currentMatrix(), a, b, c, ca, cb, cc)
        bufferSource.endBatch(renderType)
    }

    fun drawQuadDirect(a: Vec3, b: Vec3, c: Vec3, d: Vec3, color: Int, depthTest: Boolean) {
        drawQuad(a, b, c, d, color, depthTest)
    }

    fun drawQuadGradientDirect(a: Vec3, b: Vec3, c: Vec3, d: Vec3, ca: Int, cb: Int, cc: Int, cd: Int, depthTest: Boolean) {
        val renderType = RenderLayers.quads(depthTest)
        quadRenderer.drawQuad(renderType, currentMatrix(), a, b, c, d, ca, cb, cc, cd)
        bufferSource.endBatch(renderType)
    }

    fun render(commands: List<RenderCommand3D>) {
        // Rust 側のコマンドをフラッシュして直接レンダリング
        val binaryCommands = org.infinite.nativebind.com.mgpu3d.Flush.flush()
        if (binaryCommands.isNotEmpty()) {
            println("Mgpu3D Flush: processing ${binaryCommands.size} bytes")
            org.infinite.libs.mgpu3d.Mgpu3DParser.process(binaryCommands, this)
        }

        // 2. Kotlin 側の既存のコマンドをレンダリング
        val usedRenderTypes = LinkedHashSet<RenderType>()
        commands.forEach { c ->
            when (c) {
                is RenderCommand3D.Line -> drawLine(c.from, c.to, c.color, c.size, c.depthTest)

                is RenderCommand3D.Quad -> drawQuad(c.a, c.b, c.c, c.d, c.color, c.depthTest)

                is RenderCommand3D.QuadFill -> drawQuadFill(c.a, c.b, c.c, c.d, c.color, c.depthTest)

                is RenderCommand3D.QuadFillGradient -> {
                    val renderType = RenderLayers.quads(c.depthTest)
                    quadRenderer.drawQuad(
                        renderType,
                        currentMatrix(c.modelMatrix),
                        c.a,
                        c.b,
                        c.c,
                        c.d,
                        c.colorA,
                        c.colorB,
                        c.colorC,
                        c.colorD,
                    )
                    usedRenderTypes.add(renderType)
                }

                is RenderCommand3D.QuadTextured -> {
                    val renderType = RenderTypes.entityTranslucent(c.texture)
                    texturedRenderer.drawQuad(
                        renderType,
                        currentMatrix(c.modelMatrix),
                        c.a,
                        c.b,
                        c.c,
                        c.d,
                        OverlayTexture.NO_OVERLAY,
                        LightTexture.FULL_BRIGHT,
                    )
                    usedRenderTypes.add(renderType)
                }

                is RenderCommand3D.Triangle -> drawTriangle(c.a, c.b, c.c, c.color, c.depthTest)

                is RenderCommand3D.TriangleFill -> drawTriangleFill(c.a, c.b, c.c, c.color, c.depthTest)

                is RenderCommand3D.TriangleFillGradient -> {
                    val renderType = RenderLayers.quads(c.depthTest)
                    quadRenderer.drawTriangle(
                        renderType,
                        currentMatrix(c.modelMatrix),
                        c.a,
                        c.b,
                        c.c,
                        c.colorA,
                        c.colorB,
                        c.colorC,
                    )
                    usedRenderTypes.add(renderType)
                }

                is RenderCommand3D.TriangleTextured -> {
                    val renderType = RenderTypes.entityTranslucent(c.texture)
                    texturedRenderer.drawTriangle(
                        renderType,
                        currentMatrix(c.modelMatrix),
                        c.a,
                        c.b,
                        c.c,
                        OverlayTexture.NO_OVERLAY,
                        LightTexture.FULL_BRIGHT,
                    )
                    usedRenderTypes.add(renderType)
                }

                is RenderCommand3D.MeshBuffer -> {
                    val mat = currentMatrix(c.modelMatrix)
                    // 1. Lines Rendering
                    c.lineBuffer?.let { buffer ->
                        val renderType = RenderLayers.lines(false)
                        val consumer = bufferSource.getBuffer(renderType)
                        var cursor = 0L
                        while (cursor < c.lineBufferSize) {
                            val x1 = buffer.get(ValueLayout.JAVA_FLOAT, cursor * 4)
                            val y1 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 1) * 4)
                            val z1 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 2) * 4)
                            val x2 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 3) * 4)
                            val y2 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 4) * 4)
                            val z2 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 5) * 4)
                            val color = buffer.get(ValueLayout.JAVA_INT, (cursor + 6) * 4)

                            val a = (color ushr 24) and 0xFF
                            val r = (color shr 16) and 0xFF
                            val g = (color shr 8) and 0xFF
                            val b = color and 0xFF

                            consumer.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
                                .setLineWidth(2.0f)
                            consumer.addVertex(mat, x2, y2, z2).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
                                .setLineWidth(2.0f)
                            cursor += 7
                        }
                        usedRenderTypes.add(renderType)
                    }

                    // 2. Quads Rendering
                    c.quadBuffer?.let { buffer ->
                        val renderType = RenderLayers.quads(false)
                        val consumer = bufferSource.getBuffer(renderType)
                        var cursor = 0L
                        while (cursor < c.quadBufferSize) {
                            val x1 = buffer.get(ValueLayout.JAVA_FLOAT, cursor * 4)
                            val y1 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 1) * 4)
                            val z1 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 2) * 4)
                            val x2 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 3) * 4)
                            val y2 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 4) * 4)
                            val z2 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 5) * 4)
                            val x3 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 6) * 4)
                            val y3 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 7) * 4)
                            val z3 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 8) * 4)
                            val x4 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 9) * 4)
                            val y4 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 10) * 4)
                            val z4 = buffer.get(ValueLayout.JAVA_FLOAT, (cursor + 11) * 4)
                            val color = buffer.get(ValueLayout.JAVA_INT, (cursor + 12) * 4)

                            val a = (color ushr 24) and 0xFF
                            val r = (color shr 16) and 0xFF
                            val g = (color shr 8) and 0xFF
                            val b = color and 0xFF

                            consumer.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
                            consumer.addVertex(mat, x2, y2, z2).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
                            consumer.addVertex(mat, x3, y3, z3).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
                            consumer.addVertex(mat, x4, y4, z4).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
                            cursor += 13
                        }
                        usedRenderTypes.add(renderType)
                    }
                }
            }
        }
        if (usedRenderTypes.isNotEmpty()) {
            for (renderType in usedRenderTypes) {
                bufferSource.endBatch(renderType)
            }
        }
    }

    private fun drawTriangle(a: Vec3, b: Vec3, c: Vec3, color: Int, depthTest: Boolean = true) {
        val props0 = Gizmos.line(a, b, color, 2.0f)
        val props1 = Gizmos.line(b, c, color, 2.0f)
        val props2 = Gizmos.line(c, a, color, 2.0f)
        if (!depthTest) {
            props0.setAlwaysOnTop()
            props1.setAlwaysOnTop()
            props2.setAlwaysOnTop()
        }
    }

    private fun drawTriangleFill(a: Vec3, b: Vec3, c: Vec3, color: Int, depthTest: Boolean = true) {
        val props = Gizmos.addGizmo { primitives, partialTick -> primitives.addTriangleFan(arrayOf(a, b, c), color) }
        if (!depthTest) {
            props.setAlwaysOnTop()
        }
    }

    private fun drawQuad(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        d: Vec3,
        color: Int,
        depthTest: Boolean = true,
    ) {
        val props = Gizmos.rect(a, b, c, d, GizmoStyle.stroke(color, 2.0f))
        if (!depthTest) {
            props.setAlwaysOnTop()
        }
    }

    private fun drawQuadFill(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        d: Vec3,
        color: Int,
        depthTest: Boolean = true,
    ) {
        val props = Gizmos.addGizmo { primitives, partialTick -> primitives.addQuad(a, b, c, d, color) }
        if (!depthTest) {
            props.setAlwaysOnTop()
        }
    }

    private fun currentMatrix(model: Matrix4f? = null): Matrix4f {
        if (model == null) return Matrix4f(positionMatrix)
        val resultD = Matrix4d(positionMatrix).mul(Matrix4d(model))
        return Matrix4f(resultD)
    }
}
