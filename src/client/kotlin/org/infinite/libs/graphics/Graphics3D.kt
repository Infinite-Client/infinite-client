package org.infinite.libs.graphics

import net.minecraft.client.DeltaTracker
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import org.infinite.libs.core.tick.RenderTicks
import org.infinite.libs.graphics.graphics3d.RenderSystem3D
import org.infinite.libs.graphics.graphics3d.structs.RenderCommand3D
import org.infinite.libs.graphics.graphics3d.structs.TexturedVertex
import org.infinite.libs.graphics.mesh.InfiniteMesh
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.utils.rendering.Line
import org.infinite.utils.rendering.Quad
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.*

@Suppress("unused")
class Graphics3D : MinecraftInterface() {
    private val deltaTracker: DeltaTracker by lazy { minecraft.deltaTracker }
    val gameDelta: Float get() = deltaTracker.gameTimeDeltaTicks
    val realDelta: Float get() = deltaTracker.realtimeDeltaTicks
    private val snapshot: RenderSystem3D.RenderSnapshot
        get() = RenderTicks.renderSnapShot ?: throw IllegalStateException("RenderSnapshot is not available.")

    private val commandQueue: LinkedList<RenderCommand3D> = LinkedList()

    private val modelMatrixStack = ArrayDeque<Matrix4d>().apply { add(Matrix4d()) }

    fun clear() {
        commandQueue.clear()
        modelMatrixStack.clear()
        modelMatrixStack.add(Matrix4d())
    }

    fun commands(): List<RenderCommand3D> = commandQueue.toList()

    private fun transform(position: Vec3): Vec3 {
        val model = modelMatrixStack.peekLast() ?: return position
        val vec = org.joml.Vector4d(position.x, position.y, position.z, 1.0)
        vec.mul(model)
        return Vec3(vec.x, vec.y, vec.z)
    }

    private fun transform(position: Vec3, matrix: Matrix4f): Vec3 {
        val vec = org.joml.Vector4f(position.x.toFloat(), position.y.toFloat(), position.z.toFloat(), 1.0f)
        matrix.transform(vec)
        return Vec3(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
    }

    private fun transform(vertex: TexturedVertex, matrix: Matrix4f): TexturedVertex {
        val vec = org.joml.Vector4f(vertex.position.x.toFloat(), vertex.position.y.toFloat(), vertex.position.z.toFloat(), 1.0f)
        matrix.transform(vec)
        return vertex.copy(position = Vec3(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble()))
    }

    /**
     * Projects a 3D world coordinate to 2D screen coordinates.
     * @param worldPos The position in world space.
     * @return The 2D screen position (x, y) and depth (z), or null if the projection is not possible (e.g., behind the camera).
     * The origin (0,0) is at the top-left corner of the screen.
     */
    fun project(worldPos: Vec3): Vector3f? {
        val projData = RenderTicks.latestProjectionData ?: return null

        val mvp = Matrix4f(projData.projectionMatrix).mul(projData.modelViewMatrix)
        val viewport = intArrayOf(0, 0, projData.scaledWidth, projData.scaledHeight)

        val worldVec4 = org.joml.Vector4f(worldPos.x.toFloat(), worldPos.y.toFloat(), worldPos.z.toFloat(), 1.0f)
        mvp.transform(worldVec4)
        if (worldVec4.w <= 0) return null

        val screenCoords = Vector3f()
        mvp.project(Vector3f(worldPos.x.toFloat(), worldPos.y.toFloat(), worldPos.z.toFloat()), viewport, screenCoords)

        return screenCoords
    }

    /**
     * Un-projects a 2D screen coordinate (with depth) to a 3D world coordinate.
     * @param screenPos The 2D screen position (x, y) and depth (z, from 0.0 for near plane to 1.0 for far plane).
     * @return The 3D position in world space, or null if un-projection is not possible.
     */
    fun unproject(screenPos: Vector3f): Vec3 {
        val projData = RenderTicks.latestProjectionData ?: throw IllegalStateException("Projection data is not available")

        val mvp = Matrix4f(projData.projectionMatrix).mul(projData.modelViewMatrix)
        val viewport = intArrayOf(0, 0, projData.scaledWidth, projData.scaledHeight)
        val worldCoords = Vector3f()

        mvp.unproject(screenPos, viewport, worldCoords)
        return Vec3(worldCoords.x.toDouble(), worldCoords.y.toDouble(), worldCoords.z.toDouble())
    }

    private fun getModelViewMatrix(): Matrix4f {
        val projData = RenderTicks.latestProjectionData ?: return Matrix4f(modelMatrixStack.peekLast() ?: Matrix4d())
        val model = modelMatrixStack.peekLast() ?: Matrix4d()
        return Matrix4f(projData.modelViewMatrix).mul(Matrix4f(model))
    }

    fun mesh(mesh: InfiniteMesh) {
        commandQueue.add(
            RenderCommand3D.MeshBuffer(
                mesh.getLineBuffer(),
                mesh.getLineBufferSize(),
                mesh.getQuadBuffer(),
                mesh.getQuadBufferSize(),
                getModelViewMatrix(),
            ),
        )
    }

    fun renderSolidQuads(quads: List<Quad>, depthTest: Boolean = true) {
        quads.forEach { q ->
            rectangleFill(q.vertex1, q.vertex2, q.vertex3, q.vertex4, q.color, depthTest)
        }
    }

    fun renderLinedLines(lines: List<Line>, depthTest: Boolean = true) {
        lines.forEach { l ->
            line(l.start, l.end, l.color, 1.0f, depthTest)
        }
    }

    fun line(start: Vec3, end: Vec3, color: Int, size: Float = 1.0f, depthTest: Boolean = true) {
        commandQueue.add(RenderCommand3D.Line(transform(start), transform(end), color, size, depthTest))
    }

    fun triangleFill(a: Vec3, b: Vec3, c: Vec3, color: Int, depthTest: Boolean = true) {
        commandQueue.add(RenderCommand3D.TriangleFill(transform(a), transform(b), transform(c), color, depthTest))
    }

    fun triangleFill(a: Vec3, b: Vec3, c: Vec3, colorA: Int, colorB: Int, colorC: Int, depthTest: Boolean = true) {
        val modelView = getModelViewMatrix()
        commandQueue.add(
            RenderCommand3D.TriangleFillGradient(
                transform(a, modelView),
                transform(b, modelView),
                transform(c, modelView),
                colorA,
                colorB,
                colorC,
                depthTest,
            ),
        )
    }

    fun triangleFrame(a: Vec3, b: Vec3, c: Vec3, color: Int, size: Float = 1.0f, depthTest: Boolean = true) {
        line(a, b, color, size, depthTest)
        line(b, c, color, size, depthTest)
        line(c, a, color, size, depthTest)
    }

    fun rectangleFill(a: Vec3, b: Vec3, c: Vec3, d: Vec3, color: Int, depthTest: Boolean = true) {
        commandQueue.add(RenderCommand3D.QuadFill(transform(a), transform(b), transform(c), transform(d), color, depthTest))
    }

    fun rectangleFill(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        d: Vec3,
        colorA: Int,
        colorB: Int,
        colorC: Int,
        colorD: Int,
        depthTest: Boolean = true,
    ) {
        val modelView = getModelViewMatrix()
        commandQueue.add(
            RenderCommand3D.QuadFillGradient(
                transform(a, modelView),
                transform(b, modelView),
                transform(c, modelView),
                transform(d, modelView),
                colorA, colorB, colorC, colorD, depthTest,
            ),
        )
    }

    fun triangleTexture(a: TexturedVertex, b: TexturedVertex, c: TexturedVertex, texture: Identifier, depthTest: Boolean = true) {
        val modelView = getModelViewMatrix()
        commandQueue.add(
            RenderCommand3D.TriangleTextured(
                transform(a, modelView),
                transform(b, modelView),
                transform(c, modelView),
                texture,
                depthTest,
            ),
        )
    }

    fun rectangleTexture(
        a: TexturedVertex,
        b: TexturedVertex,
        c: TexturedVertex,
        d: TexturedVertex,
        texture: Identifier,
        depthTest: Boolean = true,
    ) {
        val modelView = getModelViewMatrix()
        commandQueue.add(
            RenderCommand3D.QuadTextured(
                transform(a, modelView),
                transform(b, modelView),
                transform(c, modelView),
                transform(d, modelView),
                texture,
                depthTest,
            ),
        )
    }

    fun rectangleFrame(a: Vec3, b: Vec3, c: Vec3, d: Vec3, color: Int, size: Float = 1.0f, depthTest: Boolean = true) {
        line(a, b, color, size, depthTest)
        line(b, c, color, size, depthTest)
        line(c, d, color, size, depthTest)
        line(d, a, color, size, depthTest)
    }

    fun boxOptimized(min: Vec3, max: Vec3, color: Int, size: Float = 1.0f, depthTest: Boolean = true) {
        val x0 = min.x
        val y0 = min.y
        val z0 = min.z
        val x1 = max.x
        val y1 = max.y
        val z1 = max.z
        val v000 = Vec3(x0, y0, z0)
        val v001 = Vec3(x0, y0, z1)
        val v010 = Vec3(x0, y1, z0)
        val v011 = Vec3(x0, y1, z1)
        val v100 = Vec3(x1, y0, z0)
        val v101 = Vec3(x1, y0, z1)
        val v110 = Vec3(x1, y1, z0)
        val v111 = Vec3(x1, y1, z1)
        line(v000, v001, color, size, depthTest)
        line(v001, v011, color, size, depthTest)
        line(v011, v010, color, size, depthTest)
        line(v010, v000, color, size, depthTest)
        line(v100, v101, color, size, depthTest)
        line(v101, v111, color, size, depthTest)
        line(v111, v110, color, size, depthTest)
        line(v110, v100, color, size, depthTest)
        line(v000, v100, color, size, depthTest)
        line(v001, v101, color, size, depthTest)
        line(v010, v110, color, size, depthTest)
        line(v011, v111, color, size, depthTest)
    }

    fun triangle(a: Vec3, b: Vec3, c: Vec3, color: Int, depthTest: Boolean = true) {
        commandQueue.add(RenderCommand3D.Triangle(transform(a), transform(b), transform(c), color, depthTest))
    }

    fun quad(a: Vec3, b: Vec3, c: Vec3, d: Vec3, color: Int, depthTest: Boolean = true) {
        commandQueue.add(RenderCommand3D.Quad(transform(a), transform(b), transform(c), transform(d), color, depthTest))
    }

    fun setMatrix(matrix: Matrix4f) {
        if (modelMatrixStack.isNotEmpty()) {
            modelMatrixStack.removeLast()
        }
        modelMatrixStack.add(Matrix4d(matrix))
    }

    fun pushMatrix() {
        val current = modelMatrixStack.peekLast() ?: Matrix4d()
        modelMatrixStack.add(Matrix4d(current))
    }

    fun popMatrix() {
        if (modelMatrixStack.size > 1) {
            modelMatrixStack.removeLast()
        }
    }
}
