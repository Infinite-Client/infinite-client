package org.infinite.libs.graphics.graphics3d

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.resource.GraphicsResourceAllocator
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.gizmos.Gizmo
import net.minecraft.gizmos.GizmoPrimitives
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.gizmos.Gizmos
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.graphics3d.structs.RenderCommand3D
import org.infinite.libs.graphics.graphics3d.system.QuadRenderer
import org.infinite.libs.graphics.graphics3d.system.TexturedRenderer
import org.infinite.libs.graphics.graphics3d.system.resource.RenderLayers
import org.infinite.libs.interfaces.MinecraftInterface
import org.joml.Matrix4f
import org.joml.Vector4f
import java.util.ArrayDeque

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
    private val modelMatrixStack = ArrayDeque<Matrix4f>().apply { add(Matrix4f()) }
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
        val from = transform(start)
        val to = transform(end)
        val props = Gizmos.line(from, to, color, lineWidth)
        if (!depthTest) {
            props.setAlwaysOnTop()
        }
    }

    fun render(commands: List<RenderCommand3D>) {
        val usedRenderTypes = LinkedHashSet<RenderType>()
        for (i in 0 until commands.size) {
            when (val c = commands[i]) {
                is RenderCommand3D.Line -> drawLine(c.from, c.to, c.color, c.size, c.depthTest)

                RenderCommand3D.PopMatrix -> popMatrix()

                RenderCommand3D.PushMatrix -> pushMatrix()

                is RenderCommand3D.Quad -> drawQuad(c.a, c.b, c.c, c.d, c.color, c.depthTest)

                is RenderCommand3D.QuadFill -> drawQuadFill(c.a, c.b, c.c, c.d, c.color, c.depthTest)

                is RenderCommand3D.QuadFillGradient -> {
                    val renderType = RenderLayers.quads(c.depthTest)
                    quadRenderer.drawQuad(
                        renderType,
                        currentMatrix(),
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
                        currentMatrix(),
                        c.a,
                        c.b,
                        c.c,
                        c.d,
                        OverlayTexture.NO_OVERLAY,
                        LightTexture.FULL_BRIGHT,
                    )
                    usedRenderTypes.add(renderType)
                }

                is RenderCommand3D.SetMatrix -> setMatrix(c.matrix)

                is RenderCommand3D.Triangle -> drawTriangle(c.a, c.b, c.c, c.color, c.depthTest)

                is RenderCommand3D.TriangleFill -> drawTriangleFill(c.a, c.b, c.c, c.color, c.depthTest)

                is RenderCommand3D.TriangleFillGradient -> {
                    val renderType = RenderLayers.quads(c.depthTest)
                    quadRenderer.drawTriangle(
                        renderType,
                        currentMatrix(),
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
                        currentMatrix(),
                        c.a,
                        c.b,
                        c.c,
                        OverlayTexture.NO_OVERLAY,
                        LightTexture.FULL_BRIGHT,
                    )
                    usedRenderTypes.add(renderType)
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
        val p0 = transform(a)
        val p1 = transform(b)
        val p2 = transform(c)
        val props0 = Gizmos.line(p0, p1, color, 2.0f)
        val props1 = Gizmos.line(p1, p2, color, 2.0f)
        val props2 = Gizmos.line(p2, p0, color, 2.0f)
        if (!depthTest) {
            props0.setAlwaysOnTop()
            props1.setAlwaysOnTop()
            props2.setAlwaysOnTop()
        }
    }

    private fun drawTriangleFill(a: Vec3, b: Vec3, c: Vec3, color: Int, depthTest: Boolean = true) {
        val p0 = transform(a)
        val p1 = transform(b)
        val p2 = transform(c)
        val props = Gizmos.addGizmo(
            object : Gizmo {
                override fun emit(primitives: GizmoPrimitives, partialTick: Float) {
                    primitives.addTriangleFan(arrayOf(p0, p1, p2), color)
                }
            },
        )
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
        val p0 = transform(a)
        val p1 = transform(b)
        val p2 = transform(c)
        val p3 = transform(d)
        val props = Gizmos.rect(p0, p1, p2, p3, GizmoStyle.stroke(color, 2.0f))
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
        val p0 = transform(a)
        val p1 = transform(b)
        val p2 = transform(c)
        val p3 = transform(d)
        val props = Gizmos.addGizmo(
            object : Gizmo {
                override fun emit(primitives: GizmoPrimitives, partialTick: Float) {
                    primitives.addQuad(p0, p1, p2, p3, color)
                }
            },
        )
        if (!depthTest) {
            props.setAlwaysOnTop()
        }
    }

    private fun transform(position: Vec3): Vec3 {
        val model = modelMatrixStack.peekLast() ?: Matrix4f()
        val vec = Vector4f(
            position.x.toFloat(),
            position.y.toFloat(),
            position.z.toFloat(),
            1.0f,
        ).mul(model)
        return Vec3(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
    }

    private fun pushMatrix() {
        val current = modelMatrixStack.peekLast() ?: Matrix4f()
        modelMatrixStack.add(Matrix4f(current))
    }

    private fun popMatrix() {
        if (modelMatrixStack.size > 1) {
            modelMatrixStack.removeLast()
        }
    }

    private fun setMatrix(matrix: Matrix4f) {
        if (modelMatrixStack.isNotEmpty()) {
            modelMatrixStack.removeLast()
        }
        modelMatrixStack.add(Matrix4f(matrix))
    }

    private fun currentMatrix(): Matrix4f {
        val model = modelMatrixStack.peekLast() ?: Matrix4f()
        return Matrix4f(positionMatrix).mul(model)
    }
}
