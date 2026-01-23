package org.infinite.libs.graphics.graphics3d.system

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.sqrt

class QuadRenderer(
    private val bufferSource: MultiBufferSource.BufferSource,
) {
    fun drawTriangle(
        renderType: RenderType,
        matrix: Matrix4f,
        a: Vec3,
        b: Vec3,
        c: Vec3,
        color: Int,
    ) {
        val consumer = bufferSource.getBuffer(renderType)
        val c0 = colorComponents(color)
        val n = triangleNormal(a, b, c)
        addVertex(consumer, matrix, a, c0, n)
        addVertex(consumer, matrix, b, c0, n)
        addVertex(consumer, matrix, c, c0, n)
    }

    fun drawQuad(
        renderType: RenderType,
        matrix: Matrix4f,
        a: Vec3,
        b: Vec3,
        c: Vec3,
        d: Vec3,
        color: Int,
    ) {
        drawTriangle(renderType, matrix, a, b, c, color)
        drawTriangle(renderType, matrix, a, c, d, color)
    }

    private data class ColorComponents(val r: Int, val g: Int, val b: Int, val a: Int)
    private data class Normal(val x: Float, val y: Float, val z: Float)

    private fun addVertex(
        consumer: VertexConsumer,
        matrix: Matrix4f,
        pos: Vec3,
        color: ColorComponents,
        normal: Normal,
    ) {
        consumer.addVertex(matrix, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
            .setColor(color.r, color.g, color.b, color.a)
            .setNormal(normal.x, normal.y, normal.z)
    }

    private fun colorComponents(color: Int): ColorComponents {
        val a = (color ushr 24) and 0xFF
        val r = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val b = color and 0xFF
        return ColorComponents(r, g, b, a)
    }

    private fun triangleNormal(a: Vec3, b: Vec3, c: Vec3): Normal {
        val abx = (b.x - a.x).toFloat()
        val aby = (b.y - a.y).toFloat()
        val abz = (b.z - a.z).toFloat()
        val acx = (c.x - a.x).toFloat()
        val acy = (c.y - a.y).toFloat()
        val acz = (c.z - a.z).toFloat()

        val nx = aby * acz - abz * acy
        val ny = abz * acx - abx * acz
        val nz = abx * acy - aby * acx
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        if (length <= 0.0f) {
            return Normal(0.0f, 1.0f, 0.0f)
        }
        return Normal(nx / length, ny / length, nz / length)
    }
}
