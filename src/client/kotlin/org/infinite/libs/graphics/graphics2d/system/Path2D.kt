package org.infinite.libs.graphics.graphics2d.system

import org.infinite.libs.graphics.graphics2d.structs.FillRule
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.nativebind.XrossFillRule
import org.infinite.nativebind.XrossLineCap
import org.infinite.nativebind.XrossLineJoin
import java.lang.foreign.ValueLayout
import org.infinite.nativebind.Path2D as NativePath2D

class Path2D : AutoCloseable {
    private val native = NativePath2D()

    fun beginPath() = native.begin()

    private fun syncPen(style: StrokeStyle) {
        native.setPen(
            style.width.toDouble(),
            style.color,
            XrossLineCap.entries[style.lineCap.ordinal],
            XrossLineJoin.entries[style.lineJoin.ordinal],
            style.enabledGradient,
        )
    }

    fun moveTo(x: Float, y: Float, style: StrokeStyle) {
        syncPen(style)
        native.moveTo(x.toDouble(), y.toDouble())
    }

    fun lineTo(x: Float, y: Float, style: StrokeStyle) {
        syncPen(style)
        native.lineTo(x.toDouble(), y.toDouble())
    }

    fun bezierCurveTo(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float, x: Float, y: Float, style: StrokeStyle) {
        syncPen(style)
        native.bezierCurveTo(
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
        native.quadraticCurveTo(
            cpx.toDouble(),
            cpy.toDouble(),
            x.toDouble(),
            y.toDouble(),
        )
    }

    fun arc(x: Float, y: Float, r: Float, startA: Float, endA: Float, ccw: Boolean, style: StrokeStyle) {
        syncPen(style)
        native.arc(
            x.toDouble(),
            y.toDouble(),
            r.toDouble(),
            startA.toDouble(),
            endA.toDouble(),
            ccw,
        )
    }

    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float, style: StrokeStyle) {
        syncPen(style)
        native.arcTo(
            x1.toDouble(),
            y1.toDouble(),
            x2.toDouble(),
            y2.toDouble(),
            radius.toDouble(),
        )
    }

    fun closePath() {
        native.closePath()
    }

    fun fillPath(
        fillRule: FillRule,
        color: Int,
        fillTriangle: (Float, Float, Float, Float, Float, Float, Int, Int, Int) -> Unit,
        fillQuad: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        // 塗りつぶし用の色を同期 (width=0, cap/joinはデフォルトの[0]を使用)
        native.setPen(0.0, color, XrossLineCap.entries[0], XrossLineJoin.entries[0], false)
        native.tessellateFill(XrossFillRule.entries[fillRule.ordinal])
        processBuffer(fillTriangle, fillQuad)
    }

    fun strokePath(
        style: StrokeStyle,
        draw: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        syncPen(style)
        native.tessellateStroke()
        processBuffer({ _, _, _, _, _, _, _, _, _ -> }, draw)
    }

    private fun processBuffer(
        fillTriangle: (Float, Float, Float, Float, Float, Float, Int, Int, Int) -> Unit,
        fillQuad: (Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, Int) -> Unit,
    ) {
        val sizeVal = native.getBufferSize()
        if (sizeVal <= 0L) return

        val bufferPtr = native.getBufferPtr()
        val segment = bufferPtr.reinterpret(sizeVal * 4)

        var cursorIdx = 0L
        while (cursorIdx < sizeVal) {
            val typeId = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx * 4).toInt()
            cursorIdx++

            when (typeId) {
                3 -> { // Triangle
                    val tx0 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val ty0 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val tx1 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val ty1 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val tx2 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val ty2 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val tc0 = segment.get(ValueLayout.JAVA_INT, cursorIdx++ * 4)
                    val tc1 = segment.get(ValueLayout.JAVA_INT, cursorIdx++ * 4)
                    val tc2 = segment.get(ValueLayout.JAVA_INT, cursorIdx++ * 4)
                    fillTriangle(tx0, ty0, tx1, ty1, tx2, ty2, tc0, tc1, tc2)
                }

                4 -> { // Quad
                    val qx0 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val qy0 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val qx1 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val qy1 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val qx2 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val qy2 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val qx3 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val qy3 = segment.get(ValueLayout.JAVA_FLOAT, cursorIdx++ * 4)
                    val qc0 = segment.get(ValueLayout.JAVA_INT, cursorIdx++ * 4)
                    val qc1 = segment.get(ValueLayout.JAVA_INT, cursorIdx++ * 4)
                    val qc2 = segment.get(ValueLayout.JAVA_INT, cursorIdx++ * 4)
                    val qc3 = segment.get(ValueLayout.JAVA_INT, cursorIdx++ * 4)
                    fillQuad(qx0, qy0, qx1, qy1, qx2, qy2, qx3, qy3, qc0, qc1, qc2, qc3)
                }
            }
        }
    }

    override fun close() {
        native.close()
    }
}
