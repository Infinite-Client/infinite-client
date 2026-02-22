package org.infinite.libs.graphics.graphics3d.system

import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.graphics3d.RenderSystem3D
import org.infinite.libs.graphics.graphics3d.structs.TexturedVertex
import org.infinite.nativebind.xross.runtime.XrossByteArrayView

class NativeParser3D(private val buffer: XrossByteArrayView) {
    private var offset = 0L

    companion object {
        // Rust側の Command3D::signature と一致させる
        const val SIG_LINE: Byte = 0
        const val SIG_TRIANGLE: Byte = 1
        const val SIG_TRIANGLE_FILL: Byte = 2
        const val SIG_TRIANGLE_FILL_GRADIENT: Byte = 3
        const val SIG_QUAD: Byte = 4
        const val SIG_QUAD_FILL: Byte = 5
        const val SIG_QUAD_FILL_GRADIENT: Byte = 6
        const val SIG_TRIANGLE_TEXTURED: Byte = 7
        const val SIG_QUAD_TEXTURED: Byte = 8
    }

    /**
     * バッファ全体を読み終わるまでパースを継続
     */
    fun process(system3D: RenderSystem3D) {
        offset = 0L
        while (offset < buffer.size) {
            next(system3D)
        }
    }

    private fun next(system3D: RenderSystem3D) {
        when (val signature = readByte()) {
            SIG_LINE -> parseLine(system3D)
            SIG_TRIANGLE -> parseTriangle(system3D, fill = false)
            SIG_TRIANGLE_FILL -> parseTriangle(system3D, fill = true)
            SIG_TRIANGLE_FILL_GRADIENT -> parseTriangleFillGradient(system3D)
            SIG_QUAD -> parseQuad(system3D, fill = false)
            SIG_QUAD_FILL -> parseQuad(system3D, fill = true)
            SIG_QUAD_FILL_GRADIENT -> parseQuadFillGradient(system3D)
            SIG_TRIANGLE_TEXTURED -> parseTriangleTextured(system3D)
            SIG_QUAD_TEXTURED -> parseQuadTextured(system3D)
            else -> throw IllegalStateException("Unknown 3D command signature: $signature at offset $offset")
        }
    }

    // --- 各コマンドのパース処理 ---

    private fun parseLine(system3D: RenderSystem3D) {
        val from = readVec3()
        val to = readVec3()
        val color = readInt()
        val size = readFloat()
        val depthTest = readBoolean()
        system3D.drawLine(from, to, color, size, depthTest)
    }

    private fun parseTriangle(system3D: RenderSystem3D, fill: Boolean) {
        val a = readVec3()
        val b = readVec3()
        val c = readVec3()
        val color = readInt()
        val depthTest = readBoolean()

        if (fill) {
            system3D.drawTriangleFill(a, b, c, color, depthTest)
        } else {
            system3D.drawTriangle(a, b, c, color, depthTest)
        }
    }

    private fun parseTriangleFillGradient(system3D: RenderSystem3D) {
        val a = readVec3()
        val b = readVec3()
        val c = readVec3()
        val colorA = readInt()
        val colorB = readInt()
        val colorC = readInt()
        val depthTest = readBoolean()
        system3D.drawTriangle(a, b, c, colorA, colorB, colorC, depthTest)
    }

    private fun parseQuad(system3D: RenderSystem3D, fill: Boolean) {
        val a = readVec3()
        val b = readVec3()
        val c = readVec3()
        val d = readVec3()
        val color = readInt()
        val depthTest = readBoolean()

        if (fill) {
            system3D.drawQuadFill(a, b, c, d, color, depthTest)
        } else {
            system3D.drawQuad(a, b, c, d, color, depthTest)
        }
    }

    private fun parseQuadFillGradient(system3D: RenderSystem3D) {
        val a = readVec3()
        val b = readVec3()
        val c = readVec3()
        val d = readVec3()
        val colorA = readInt()
        val colorB = readInt()
        val colorC = readInt()
        val colorD = readInt()
        val depthTest = readBoolean()
        system3D.drawQuadFill(a, b, c, d, colorA, colorB, colorC, colorD, depthTest)
    }

    private fun parseTriangleTextured(system3D: RenderSystem3D) {
        // TexturedVertex a, b, c
        val va = readTexturedVertex()
        val vb = readTexturedVertex()
        val vc = readTexturedVertex()
        val texture = readIdentifier()
        val depthTest = readBoolean()
        system3D.drawTriangleTextured(va, vb, vc, texture, depthTest)
    }

    private fun parseQuadTextured(system3D: RenderSystem3D) {
        val va = readTexturedVertex()
        val vb = readTexturedVertex()
        val vc = readTexturedVertex()
        val vd = readTexturedVertex()
        val texture = readIdentifier()
        val depthTest = readBoolean()
        system3D.drawQuadTextured(va, vb, vc, vd, texture, depthTest)
    }

    // --- 低レベル読み込み補助 ---

    private fun readByte(): Byte = buffer[offset++]

    private fun readBoolean(): Boolean = readByte() != 0.toByte()

    private fun readInt(): Int {
        val res = (buffer[offset].toInt() and 0xFF) or
            ((buffer[offset + 1].toInt() and 0xFF) shl 8) or
            ((buffer[offset + 2].toInt() and 0xFF) shl 16) or
            ((buffer[offset + 3].toInt() and 0xFF) shl 24)
        offset += 4
        return res
    }

    private fun readFloat(): Float = Float.fromBits(readInt())

    private fun readDouble(): Double {
        val low = readInt().toLong() and 0xFFFFFFFFL
        val high = readInt().toLong() and 0xFFFFFFFFL
        return Double.fromBits((high shl 32) or low)
    }

    private fun readVec3(): Vec3 = Vec3(readDouble(), readDouble(), readDouble())

    private fun readIdentifier(): Identifier {
        val len = readInt()
        val bytes = ByteArray(len)
        for (i in 0 until len) {
            bytes[i] = buffer[offset++]
        }
        return Identifier.parse(String(bytes))
    }

    private fun readTexturedVertex(): TexturedVertex {
        val pos = readVec3()
        val u = readDouble()
        val v = readDouble()
        val color = readInt()
        return TexturedVertex(pos, u.toFloat(), v.toFloat(), color)
    }
}
