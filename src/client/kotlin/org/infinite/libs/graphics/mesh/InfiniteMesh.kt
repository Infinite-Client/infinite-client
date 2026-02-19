package org.infinite.libs.graphics.mesh

import net.minecraft.world.phys.Vec3
import java.lang.foreign.MemorySegment

class InfiniteMesh : AutoCloseable {
    fun clear() {}

    fun addBox(pos: Vec3, size: Vec3, color: Int, lines: Boolean = true) {}

    fun addLine(start: Vec3, end: Vec3, color: Int) {}

    fun getLineBuffer(): MemorySegment? = null
    fun getLineBufferSize(): Long = 0L

    fun getQuadBuffer(): MemorySegment? = null
    fun getQuadBufferSize(): Long = 0L

    override fun close() {}
}
