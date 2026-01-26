package org.infinite.libs.graphics.graphics2d.system

import org.infinite.libs.graphics.graphics2d.structs.FillRule
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.nativebind.infinite_client_h
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

class Path2D : AutoCloseable {
    private var nativePtr = infinite_client_h.graphics2d_path2d_new()

    fun beginPath() = infinite_client_h.graphics2d_path2d_clear(nativePtr)

    fun moveTo(x: Float, y: Float) = infinite_client_h.graphics2d_path2d_add_point(nativePtr, x, y, 0, 0f, 0)

    fun lineTo(x: Float, y: Float, style: StrokeStyle) = infinite_client_h.graphics2d_path2d_add_point(nativePtr, x, y, style.color, style.width, 1)

    fun closePath(style: StrokeStyle) = infinite_client_h.graphics2d_path2d_add_point(nativePtr, 0f, 0f, style.color, style.width, 2)

    fun fillPath(
        fillRule: FillRule,
        fillTriangle: (Float, Float, Float, Float, Float, Float, Int, Int, Int) -> Unit,
        fillQuad: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        infinite_client_h.graphics2d_path2d_tessellate_fill(nativePtr, fillRule.ordinal)
        processBuffer(fillTriangle, fillQuad)
    }

    fun strokePath(
        fillQuad: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        infinite_client_h.graphics2d_path2d_tessellate_stroke(nativePtr)
        processBuffer({ _, _, _, _, _, _, _, _, _ -> }, fillQuad)
    }

    private fun processBuffer(
        fillTriangle: (Float, Float, Float, Float, Float, Float, Int, Int, Int) -> Unit,
        fillQuad: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        val size = infinite_client_h.graphics2d_path2d_get_buffer_size(nativePtr)
        if (size <= 0) return

        val bufferPtr = infinite_client_h.graphics2d_path2d_get_buffer_ptr(nativePtr)
        val segment = bufferPtr.reinterpret(size.toLong() * 4)

        var cursor = 0L
        while (cursor < size) {
            val type = segment.get(ValueLayout.JAVA_FLOAT, cursor * 4).toInt()
            cursor++

            if (type == 3) { // Triangle: [x0, y0, x1, y1, x2, y2, c0, c1, c2]
                val x0 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val y0 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val x1 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val y1 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val x2 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val y2 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)

                val c0 = segment.get(ValueLayout.JAVA_INT, cursor++ * 4)
                val c1 = segment.get(ValueLayout.JAVA_INT, cursor++ * 4)
                val c2 = segment.get(ValueLayout.JAVA_INT, cursor++ * 4)

                fillTriangle(x0, y0, x1, y1, x2, y2, c0, c1, c2)
            } else if (type == 4) { // Quad: [x0, y0, x1, y1, x2, y2, x3, y3, c0, c1, c2, c3]
                val x0 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val y0 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val x1 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val y1 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val x2 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val y2 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val x3 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)
                val y3 = segment.get(ValueLayout.JAVA_FLOAT, cursor++ * 4)

                val c0 = segment.get(ValueLayout.JAVA_INT, cursor++ * 4)
                val c1 = segment.get(ValueLayout.JAVA_INT, cursor++ * 4)
                val c2 = segment.get(ValueLayout.JAVA_INT, cursor++ * 4)
                val c3 = segment.get(ValueLayout.JAVA_INT, cursor++ * 4)

                fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, c0, c1, c2, c3)
            }
        }
    }

    override fun close() {
        if (nativePtr != MemorySegment.NULL) {
            infinite_client_h.graphics2d_path2d_drop(nativePtr)
            nativePtr = MemorySegment.NULL
        }
    }
    fun arc(x: Float, y: Float, r: Float, startA: Float, endA: Float, ccw: Boolean, style: StrokeStyle) {
        infinite_client_h.graphics2d_path2d_arc(nativePtr, x, y, r, startA, endA, ccw, style.color, style.width)
    }
    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float, style: StrokeStyle) {
        infinite_client_h.graphics2d_path2d_arc_to(nativePtr, x1, y1, x2, y2, radius, style.color, style.width)
    }
    fun bezierCurveTo(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float, x: Float, y: Float, style: StrokeStyle) {
        infinite_client_h.graphics2d_path2d_bezier_to(nativePtr, cp1x, cp1y, cp2x, cp2y, x, y, style.color, style.width)
    }
}
