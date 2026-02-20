package org.infinite.libs.graphics.mesh

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

class InfiniteMesh : AutoCloseable {
    private val arena = Arena.ofShared()
    private var lineBuffer: MemorySegment = MemorySegment.NULL
    private var lineSize: Long = 0L
    private var quadBuffer: MemorySegment = MemorySegment.NULL
    private var quadSize: Long = 0L

    fun updateLineData(data: FloatArray) {
        if (data.isEmpty()) {
            lineSize = 0L
            lineBuffer = MemorySegment.NULL
            return
        }
        lineSize = data.size.toLong()
        lineBuffer = arena.allocate(lineSize * 4)
        for (i in data.indices) {
            lineBuffer.set(ValueLayout.JAVA_FLOAT, i.toLong() * 4, data[i])
        }
    }

    fun updateFaceData(data: FloatArray) {
        if (data.isEmpty()) {
            quadSize = 0L
            quadBuffer = MemorySegment.NULL
            return
        }
        quadSize = data.size.toLong()
        quadBuffer = arena.allocate(quadSize * 4)
        for (i in data.indices) {
            quadBuffer.set(ValueLayout.JAVA_FLOAT, i.toLong() * 4, data[i])
        }
    }

    fun updateFromNativeMesh(
        linePtr: MemorySegment,
        lineSize: Long,
        quadPtr: MemorySegment,
        quadSize: Long,
    ) {
        if (lineSize > 0) {
            this.lineSize = lineSize
            this.lineBuffer = arena.allocate(lineSize * 4)
            this.lineBuffer.copyFrom(linePtr.reinterpret(lineSize * 4))
        } else {
            this.lineSize = 0
            this.lineBuffer = MemorySegment.NULL
        }

        if (quadSize > 0) {
            this.quadSize = quadSize
            this.quadBuffer = arena.allocate(quadSize * 4)
            this.quadBuffer.copyFrom(quadPtr.reinterpret(quadSize * 4))
        } else {
            this.quadSize = 0
            this.quadBuffer = MemorySegment.NULL
        }
    }

    fun getLineBuffer(): MemorySegment = lineBuffer
    fun getLineBufferSize(): Long = lineSize

    fun getQuadBuffer(): MemorySegment = quadBuffer
    fun getQuadBufferSize(): Long = quadSize

    override fun close() {
        arena.close()
    }
}
