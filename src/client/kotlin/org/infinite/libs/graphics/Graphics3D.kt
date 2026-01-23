package org.infinite.libs.graphics

import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import org.infinite.libs.core.tick.RenderTicks
import org.infinite.libs.graphics.graphics3d.RenderSystem3D
import org.infinite.libs.graphics.graphics3d.structs.RenderCommand3D
import org.infinite.libs.graphics.graphics3d.structs.TexturedVertex
import org.joml.Matrix4f
import java.util.LinkedList

class Graphics3D {
    // スナップショット（行列やカメラ位置）へのアクセス
    private val snapshot: RenderSystem3D.RenderSnapshot
        get() = RenderTicks.renderSnapShot ?: throw IllegalStateException("RenderSnapshot is not available.")

    private val commandQueue: LinkedList<RenderCommand3D> = LinkedList()

    fun commands(): LinkedList<RenderCommand3D> = commandQueue

    /**
     * キューをクリアします。各フレームの開始時に呼び出されることを想定しています。
     */
    fun clear() {
        commandQueue.clear()
    }

    fun line(
        start: Vec3,
        end: Vec3,
        color: Int,
        size: Float = 1.0f,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(RenderCommand3D.Line(start, end, color, size, depthTest))
    }

    fun triangleFill(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        color: Int,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(RenderCommand3D.TriangleFill(a, b, c, color, depthTest))
    }

    fun triangleFill(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        colorA: Int,
        colorB: Int,
        colorC: Int,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(
            RenderCommand3D.TriangleFillGradient(
                a,
                b,
                c,
                colorA,
                colorB,
                colorC,
                depthTest,
            ),
        )
    }

    fun triangleFrame(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        color: Int,
        size: Float = 1.0f,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(RenderCommand3D.Line(a, b, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(b, c, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(c, a, color, size, depthTest))
    }

    fun rectangleFill(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        d: Vec3,
        color: Int,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(RenderCommand3D.QuadFill(a, b, c, d, color, depthTest))
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
        commandQueue.add(
            RenderCommand3D.QuadFillGradient(
                a,
                b,
                c,
                d,
                colorA,
                colorB,
                colorC,
                colorD,
                depthTest,
            ),
        )
    }

    fun triangleTexture(
        a: TexturedVertex,
        b: TexturedVertex,
        c: TexturedVertex,
        texture: Identifier,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(RenderCommand3D.TriangleTextured(a, b, c, texture, depthTest))
    }

    fun rectangleTexture(
        a: TexturedVertex,
        b: TexturedVertex,
        c: TexturedVertex,
        d: TexturedVertex,
        texture: Identifier,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(RenderCommand3D.QuadTextured(a, b, c, d, texture, depthTest))
    }

    fun rectangleFrame(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        d: Vec3,
        color: Int,
        size: Float = 1.0f,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(RenderCommand3D.Line(a, b, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(b, c, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(c, d, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(d, a, color, size, depthTest))
    }

    fun boxOptimized(
        min: Vec3,
        max: Vec3,
        color: Int,
        size: Float = 1.0f,
        depthTest: Boolean = true,
    ) {
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

        commandQueue.add(RenderCommand3D.Line(v000, v001, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(v001, v011, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(v011, v010, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(v010, v000, color, size, depthTest))

        commandQueue.add(RenderCommand3D.Line(v100, v101, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(v101, v111, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(v111, v110, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(v110, v100, color, size, depthTest))

        commandQueue.add(RenderCommand3D.Line(v000, v100, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(v001, v101, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(v010, v110, color, size, depthTest))
        commandQueue.add(RenderCommand3D.Line(v011, v111, color, size, depthTest))
    }

    fun triangle(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        color: Int,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(RenderCommand3D.Triangle(a, b, c, color, depthTest))
    }

    fun quad(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        d: Vec3,
        color: Int,
        depthTest: Boolean = true,
    ) {
        commandQueue.add(RenderCommand3D.Quad(a, b, c, d, color, depthTest))
    }

    fun setMatrix(matrix: Matrix4f) = commandQueue.add(RenderCommand3D.SetMatrix(matrix))
    fun pushMatrix() = commandQueue.add(RenderCommand3D.PushMatrix)
    fun popMatrix() = commandQueue.add(RenderCommand3D.PopMatrix)
}
