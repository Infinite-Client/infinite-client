package org.infinite.utils.rendering

import net.minecraft.util.ARGB
import kotlin.math.roundToInt

fun getRainbowColor(value: Float? = null): Int {
    val rainbowDuration = 6000L
    val colors =
        intArrayOf(
            0xFFFF0000.toInt(),
            0xFFFFFF00.toInt(),
            0xFF00FF00.toInt(),
            0xFF00FFFF.toInt(),
            0xFF0000FF.toInt(),
            0xFFFF00FF.toInt(),
            0xFFFF0000.toInt(),
        )
    val currentTime = System.currentTimeMillis()
    val elapsedTime = currentTime % rainbowDuration
    val progress = value ?: (elapsedTime.toFloat() / rainbowDuration.toFloat())
    val numSegments = colors.size - 1
    val segmentLength = 1.0f / numSegments
    val currentSegmentIndex = (progress / segmentLength).toInt().coerceAtMost(numSegments - 1)
    val segmentProgress = (progress % segmentLength) / segmentLength
    val startColor = colors[currentSegmentIndex]
    val endColor = colors[currentSegmentIndex + 1]

    return ARGB.color(
        255,
        (ARGB.red(startColor) * (1 - segmentProgress) + ARGB.red(endColor) * segmentProgress).toInt(),
        (ARGB.green(startColor) * (1 - segmentProgress) + ARGB.green(endColor) * segmentProgress).toInt(),
        (ARGB.blue(startColor) * (1 - segmentProgress) + ARGB.blue(endColor) * segmentProgress).toInt(),
    )
}

fun Int.transparent(alpha: Int): Int {
    val alpha = alpha.coerceIn(0, 255)
    return ARGB.color(
        alpha,
        ARGB.red(
            this,
        ),
        ARGB.green(
            this,
        ),
        ARGB.blue(
            this,
        ),
    )
}

fun Int.transparent(alpha: Double): Int = this.transparent(alpha.roundToInt())

fun Int.transparent(alpha: Float): Int = this.transparent(alpha.roundToInt())
