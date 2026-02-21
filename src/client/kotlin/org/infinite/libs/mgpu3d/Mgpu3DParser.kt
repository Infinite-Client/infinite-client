package org.infinite.libs.mgpu3d

import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.graphics3d.RenderSystem3D
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Mgpu3DParser {

    /**
     * バイナリバッファをデコードしながら、指定されたレンダラーに対して直接描画命令を発行します。
     * リストを作成しないため、アロケーションコストを最小限に抑えられます。
     */
    fun process(bytes: ByteArray, renderer: RenderSystem3D) {
        if (bytes.isEmpty()) return
        println("Mgpu3DParser: processing ${bytes.size} bytes")

        // ByteBuffer の wrap は高速です
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

        while (buffer.hasRemaining()) {
            val sig = buffer.get().toInt()
            when (sig) {
                0 -> { // Line
                    val start = readVec3(buffer)
                    val end = readVec3(buffer)
                    val color = buffer.getInt()
                    val width = buffer.getFloat()
                    renderer.drawLine(start, end, color, width, true)
                }

                1 -> { // Triangle (Monochrome)
                    val a = readVec3(buffer)
                    val b = readVec3(buffer)
                    val c = readVec3(buffer)
                    val color = buffer.getInt()
                    renderer.drawTriangleDirect(a, b, c, color, true)
                }

                2 -> { // TriangleGradient
                    val a = readVec3(buffer)
                    val ca = buffer.getInt()
                    val b = readVec3(buffer)
                    val cb = buffer.getInt()
                    val c = readVec3(buffer)
                    val cc = buffer.getInt()
                    renderer.drawTriangleGradientDirect(a, b, c, ca, cb, cc, true)
                }

                3 -> { // Quad (Monochrome)
                    val a = readVec3(buffer)
                    val b = readVec3(buffer)
                    val c = readVec3(buffer)
                    val d = readVec3(buffer)
                    val color = buffer.getInt()
                    renderer.drawQuadDirect(a, b, c, d, color, true)
                }

                4 -> { // QuadGradient
                    val a = readVec3(buffer)
                    val ca = buffer.getInt()
                    val b = readVec3(buffer)
                    val cb = buffer.getInt()
                    val c = readVec3(buffer)
                    val cc = buffer.getInt()
                    val d = readVec3(buffer)
                    val cd = buffer.getInt()
                    renderer.drawQuadGradientDirect(a, b, c, d, ca, cb, cc, cd, true)
                }

                else -> break
            }
        }
    }

    private fun readVec3(buffer: ByteBuffer): Vec3 {
        // Vec3 は不変オブジェクトのため、どうしても生成が必要ですが、
        // プリミティブな値を直接 renderer に渡すように renderer 側を拡張すればさらに削れます。
        return Vec3(buffer.getDouble(), buffer.getDouble(), buffer.getDouble())
    }
}
