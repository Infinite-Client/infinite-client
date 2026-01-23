package org.infinite.libs.graphics.graphics3d.system

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.graphics3d.structs.TexturedVertex
import org.joml.Matrix4f
import kotlin.math.sqrt

class TexturedRenderer(
    private val bufferSource: MultiBufferSource.BufferSource,
) {
    fun drawTriangle(
        renderType: RenderType,
        matrix: Matrix4f,
        a: TexturedVertex,
        b: TexturedVertex,
        c: TexturedVertex,
        overlay: Int,
        light: Int,
    ) {
        val consumer = bufferSource.getBuffer(renderType)
        val overlayUv = unpack(overlay)
        val lightUv = unpack(light)
        val n = triangleNormal(a.position, b.position, c.position)
        addVertex(consumer, matrix, a, overlayUv, lightUv, n)
        addVertex(consumer, matrix, b, overlayUv, lightUv, n)
        addVertex(consumer, matrix, c, overlayUv, lightUv, n)
    }

    fun drawQuad(
        renderType: RenderType,
        matrix: Matrix4f,
        a: TexturedVertex,
        b: TexturedVertex,
        c: TexturedVertex,
        d: TexturedVertex,
        overlay: Int,
        light: Int,
    ) {
        drawTriangle(renderType, matrix, a, b, c, overlay, light)
        drawTriangle(renderType, matrix, a, c, d, overlay, light)
    }

    private data class ColorComponents(val r: Int, val g: Int, val b: Int, val a: Int)
    private data class Normal(val x: Float, val y: Float, val z: Float)
    private data class PackedUv(val u: Int, val v: Int)

    private fun addVertex(
        consumer: VertexConsumer,
        matrix: Matrix4f,
        vertex: TexturedVertex,
        overlay: PackedUv,
        light: PackedUv,
        normal: Normal,
    ) {
        val c = colorComponents(vertex.color)
        consumer.addVertex(matrix, vertex.position.x.toFloat(), vertex.position.y.toFloat(), vertex.position.z.toFloat())
            .setColor(c.r, c.g, c.b, c.a)
            .setUv(vertex.u, vertex.v)
            .setUv1(overlay.u, overlay.v)
            .setUv2(light.u, light.v)
            .setNormal(normal.x, normal.y, normal.z)
    }

    private fun unpack(packed: Int): PackedUv {
        val u = packed and 0xFFFF
        val v = packed ushr 16
        return PackedUv(u, v)
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
