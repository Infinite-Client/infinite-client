package org.infinite.utils.rendering

import net.minecraft.util.ARGB
import kotlin.math.abs
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

fun Int.alpha(alpha: Int): Int {
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

fun Int.alpha(alpha: Double): Int = this.alpha(alpha.roundToInt())

fun Int.alpha(alpha: Float): Int = this.alpha(alpha.roundToInt())

/**
 * HSLA (Alpha, Hue, Saturation, Lightness) を ARGB形式のIntに変換する。
 * @param hue   0.0 ~ 360.0 (色相)
 * @param s     0.0 ~ 1.0 (彩度)
 * @param l     0.0 ~ 1.0 (輝度)
 * @param alpha 0.0 (透明) ~ 1.0 (不透明)
 * @return ARGB形式のInt (0xAARRGGBB)
 */
fun hsla(
    hue: Float,
    s: Float,
    l: Float,
    alpha: Float,
): Int {
    // 1. 各成分を範囲内に丸める (安全策)
    val h = hue % 360f
    val sa = s.coerceIn(0f, 1f)
    val li = l.coerceIn(0f, 1f)
    val a = (alpha.coerceIn(0f, 1f) * 255 + 0.5f).toInt()

    // 2. HSL -> RGB の計算 (Chroma, X, m を使用)
    val c = (1f - abs(2f * li - 1f)) * sa
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = li - c / 2f

    val (rPrime, gPrime, bPrime) =
        when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

    // 3. 0-255のInt値に変換
    val r = ((rPrime + m) * 255 + 0.5f).toInt()
    val g = ((gPrime + m) * 255 + 0.5f).toInt()
    val b = ((bPrime + m) * 255 + 0.5f).toInt()

    // 4. ビット演算で一つのIntに結合 (ARGB)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

fun Int.red(value: Int): Int = (this and 0xFF00FFFF.toInt()) or (value shl 16)

fun Int.green(value: Int): Int = (this and 0xFFFF00FF.toInt()) or (value shl 8)

fun Int.blue(value: Int): Int = (this and 0xFFFFFF00.toInt()) or value
