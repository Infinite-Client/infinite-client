package org.infinite.utils

import org.infinite.libs.graphics.Graphics2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun roundedSegments(radius: Float): Int {
    return (radius / 3f).roundToInt().coerceIn(4, 12)
}

fun Graphics2D.fillRoundedRect(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
    segments: Int = roundedSegments(radius),
) {
    val r = radius.coerceAtLeast(0f).coerceAtMost(min(width, height) / 2f)
    if (r <= 0.5f) {
        fillRect(x, y, width, height)
        return
    }

    val innerW = width - 2f * r
    val innerH = height - 2f * r

    fillRect(x + r, y, innerW, height)
    fillRect(x, y + r, r, innerH)
    fillRect(x + width - r, y + r, r, innerH)

    val step = (PI / 2.0) / segments

    fun corner(cx: Float, cy: Float, startAngle: Double) {
        for (i in 0 until segments) {
            val a0 = startAngle + i * step
            val a1 = startAngle + (i + 1) * step
            val x0 = cx + cos(a0).toFloat() * r
            val y0 = cy + sin(a0).toFloat() * r
            val x1 = cx + cos(a1).toFloat() * r
            val y1 = cy + sin(a1).toFloat() * r
            fillTriangle(cx, cy, x0, y0, x1, y1)
        }
    }

    corner(x + r, y + r, PI)
    corner(x + width - r, y + r, 1.5 * PI)
    corner(x + width - r, y + height - r, 0.0)
    corner(x + r, y + height - r, 0.5 * PI)
}

fun Graphics2D.strokeRoundedRect(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
) {
    val r = radius.coerceAtLeast(0f).coerceAtMost(min(width, height) / 2f)
    if (r <= 0.5f) {
        strokeRect(x, y, width, height)
        return
    }

    beginPath()
    moveTo(x + r, y)
    lineTo(x + width - r, y)
    arcTo(x + width, y, x + width, y + r, r)
    lineTo(x + width, y + height - r)
    arcTo(x + width, y + height, x + width - r, y + height, r)
    lineTo(x + r, y + height)
    arcTo(x, y + height, x, y + height - r, r)
    lineTo(x, y + r)
    arcTo(x, y, x + r, y, r)
    closePath()
    strokePath()
}
