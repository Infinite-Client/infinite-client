package org.infinite.libs.graphics.graphics2d.system

import org.infinite.libs.graphics.graphics2d.structs.FillRule
import org.infinite.libs.graphics.graphics2d.structs.LineCap
import org.infinite.libs.graphics.graphics2d.structs.LineJoin
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.nativebind.infinite_client_h
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

class Path2D : AutoCloseable {
    private var nativePtr = infinite_client_h.graphics2d_path2d_new()

    // パスのリセット
    fun beginPath() = infinite_client_h.graphics2d_path2d_begin(nativePtr)

    // ペンの状態（太さ、色、キャップ、ジョイン、グラデーション）をRust側に同期
    private fun syncPen(style: StrokeStyle, cap: LineCap = LineCap.Butt, join: LineJoin = LineJoin.Miter, enableGradient: Boolean = false) {
        infinite_client_h.graphics2d_path2d_set_pen(
            nativePtr,
            style.width.toDouble(),
            style.color,
            cap.ordinal,
            join.ordinal,
            enableGradient,
        )
    }

    fun moveTo(x: Float, y: Float) {
        infinite_client_h.graphics2d_path2d_move_to(nativePtr, x.toDouble(), y.toDouble())
    }

    fun lineTo(x: Float, y: Float, style: StrokeStyle) {
        syncPen(style)
        infinite_client_h.graphics2d_path2d_line_to(nativePtr, x.toDouble(), y.toDouble())
    }

    fun bezierCurveTo(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float, x: Float, y: Float, style: StrokeStyle) {
        syncPen(style)
        infinite_client_h.graphics2d_path2d_bezier_curve_to(
            nativePtr,
            cp1x.toDouble(),
            cp1y.toDouble(),
            cp2x.toDouble(),
            cp2y.toDouble(),
            x.toDouble(),
            y.toDouble(),
        )
    }

    fun quadraticCurveTo(cpx: Float, cpy: Float, x: Float, y: Float, style: StrokeStyle) {
        syncPen(style)
        infinite_client_h.graphics2d_path2d_quadratic_curve_to(nativePtr, cpx.toDouble(), cpy.toDouble(), x.toDouble(), y.toDouble())
    }

    fun arc(x: Float, y: Float, r: Float, startA: Float, endA: Float, ccw: Boolean, style: StrokeStyle) {
        syncPen(style)
        infinite_client_h.graphics2d_path2d_arc(nativePtr, x.toDouble(), y.toDouble(), r.toDouble(), startA.toDouble(), endA.toDouble(), ccw)
    }

    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float, style: StrokeStyle) {
        syncPen(style)
        infinite_client_h.graphics2d_path2d_arc_to(nativePtr, x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), radius.toDouble())
    }

    fun closePath() {
        infinite_client_h.graphics2d_path2d_close(nativePtr)
    }

    fun fillPath(
        fillRule: FillRule,
        fillTriangle: (Float, Float, Float, Float, Float, Float, Int, Int, Int) -> Unit,
        fillQuad: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        infinite_client_h.graphics2d_path2d_tessellate_fill(nativePtr, fillRule.ordinal)
        processBuffer(fillTriangle, fillQuad)
    }

    fun strokePath(
        style: StrokeStyle,
        cap: LineCap,
        join: LineJoin,
        enableGradient: Boolean,
        draw: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        // 描画直前にペン設定を同期
        syncPen(style, cap, join, enableGradient)
        infinite_client_h.graphics2d_path2d_tessellate_stroke(nativePtr)

        // 共通のバッファ処理を利用（StrokeはQuadのみだが、汎用的に処理）
        processBuffer({ _, _, _, _, _, _, _, _, _ -> }, draw)
    }

    private fun processBuffer(
        fillTriangle: (Float, Float, Float, Float, Float, Float, Int, Int, Int) -> Unit,
        fillQuad: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        val size = infinite_client_h.graphics2d_path2d_get_buffer_size(nativePtr)
        if (size <= 0L) return

        val bufferPtr = infinite_client_h.graphics2d_path2d_get_buffer_ptr(nativePtr)
        val segment = bufferPtr.reinterpret(size * 4)

        var cursor = 0L
        while (cursor < size) {
            val type = segment.get(ValueLayout.JAVA_FLOAT, cursor * 4).toInt()
            cursor++

            when (type) {
                3 -> { // Triangle
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
                }

                4 -> { // Quad
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
    }

    override fun close() {
        if (nativePtr != MemorySegment.NULL) {
            infinite_client_h.graphics2d_path2d_drop(nativePtr)
            nativePtr = MemorySegment.NULL
        }
    }
}
