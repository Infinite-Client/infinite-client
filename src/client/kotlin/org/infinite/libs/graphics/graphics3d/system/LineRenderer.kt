package org.infinite.libs.graphics.graphics3d.system

import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.sqrt

class LineRenderer(
    private val bufferSource: MultiBufferSource.BufferSource,
) {
    fun drawLine(
        renderType: RenderType,
        matrix: Matrix4f,
        from: Vec3,
        to: Vec3,
        color: Int,
        lineWidth: Float,
    ) {
        val consumer = bufferSource.getBuffer(renderType)
        val c = colorComponents(color)
        val n = lineNormal(from, to)
        consumer.addVertex(matrix, from.x.toFloat(), from.y.toFloat(), from.z.toFloat())
            .setColor(c.r, c.g, c.b, c.a)
            .setNormal(n.x, n.y, n.z)
            .setLineWidth(lineWidth)
        consumer.addVertex(matrix, to.x.toFloat(), to.y.toFloat(), to.z.toFloat())
            .setColor(c.r, c.g, c.b, c.a)
            .setNormal(n.x, n.y, n.z)
            .setLineWidth(lineWidth)
    }

    private data class ColorComponents(val r: Int, val g: Int, val b: Int, val a: Int)
    private data class Normal(val x: Float, val y: Float, val z: Float)

    private fun colorComponents(color: Int): ColorComponents {
        val a = (color ushr 24) and 0xFF
        val r = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val b = color and 0xFF
        return ColorComponents(r, g, b, a)
    }

    private fun lineNormal(from: Vec3, to: Vec3): Normal {
        val dx = (to.x - from.x).toFloat()
        val dy = (to.y - from.y).toFloat()
        val dz = (to.z - from.z).toFloat()
        val length = sqrt(dx * dx + dy * dy + dz * dz)
        if (length <= 0.0f) {
            return Normal(0.0f, 1.0f, 0.0f)
        }
        return Normal(dx / length, dy / length, dz / length)
    }
}
