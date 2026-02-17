package org.infinite.libs.graphics.mesh

import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.Graphics3D
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import org.infinite.nativebind.InfiniteMesh as NativeMesh

class InfiniteMesh : AutoCloseable {
    private val native = NativeMesh()

    fun clear() = native.clear()

    /**
     * 指定された位置とサイズでボックスを追加します。
     * @param pos 最小座標
     * @param size ボックスのサイズ (width, height, depth)
     * @param color 色 (ARGB)
     * @param lines 線枠として描画する場合は true、面として描画する場合は false
     */
    fun addBox(pos: Vec3, size: Vec3, color: Int, lines: Boolean = true) {
        native.addBox(
            pos.x.toFloat(),
            pos.y.toFloat(),
            pos.z.toFloat(),
            size.x.toFloat(),
            size.y.toFloat(),
            size.z.toFloat(),
            color,
            lines,
        )
    }

    /**
     * 線分を追加します。
     */
    fun addLine(start: Vec3, end: Vec3, color: Int) {
        native.addLine(
            start.x.toFloat(),
            start.y.toFloat(),
            start.z.toFloat(),
            end.x.toFloat(),
            end.y.toFloat(),
            end.z.toFloat(),
            color,
        )
    }

    fun getLineBuffer(): MemorySegment? = if (native.getLineBufferSize() <= 0L) null else native.getLineBufferPtr().reinterpret(native.getLineBufferSize() * 4)
    fun getLineBufferSize(): Long = native.getLineBufferSize()

    fun getQuadBuffer(): MemorySegment? = if (native.getQuadBufferSize() <= 0L) null else native.getQuadBufferPtr().reinterpret(native.getQuadBufferSize() * 4)
    fun getQuadBufferSize(): Long = native.getQuadBufferSize()

    override fun close() {
        native.close()
    }
}
