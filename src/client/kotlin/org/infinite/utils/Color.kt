package org.infinite.utils

import kotlin.math.abs

/**
 * HSLA (Alpha, Hue, Saturation, Lightness) を ARGB形式のIntに変換する。
 * @param hue   0.0 ~ 360.0 (色相)
 * @param s     0.0 ~ 1.0 (彩度)
 * @param l     0.0 ~ 1.0 (輝度)
 * @param alpha 0.0 (透明) ~ 1.0 (不透明)
 * @return ARGB形式のInt (0xAARRGGBB)
 */
fun hsla(hue: Float, s: Float, l: Float, alpha: Float): Int {
    // 1. 各成分を範囲内に丸める (安全策)
    val h = hue % 360f
    val sa = s.coerceIn(0f, 1f)
    val li = l.coerceIn(0f, 1f)
    val a = (alpha.coerceIn(0f, 1f) * 255 + 0.5f).toInt()

    // 2. HSL -> RGB の計算 (Chroma, X, m を使用)
    val c = (1f - abs(2f * li - 1f)) * sa
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = li - c / 2f

    val (rPrime, gPrime, bPrime) = when {
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

fun Int.red(value: Int): Int = (this and 0xFF00FFFF.toInt()) or (value.coerceIn(0, 255) shl 16)
fun Int.green(value: Int): Int = (this and 0xFFFF00FF.toInt()) or (value.coerceIn(0, 255) shl 8)
fun Int.blue(value: Int): Int = (this and 0xFFFFFF00.toInt()) or value.coerceIn(0, 255)
fun Int.alpha(value: Int): Int = (this and 0x00FFFFFF) or (value.coerceIn(0, 255) shl 24)
fun Int.alpha(value: Float): Int = this.alpha((255 * value).toInt())

/**
 * 現在の色と別の色を、指定された比率で混ぜ合わせます。
 * * @param color 混ぜる対象の色 (ARGB形式)
 * @param ratio 混ぜる比率。0.0なら元の色のまま、1.0なら対象の色(color)のみ。
 * @return 混合されたARGB形式のInt
 */
fun Int.mix(color: Int, ratio: Float): Int {
    val r = ratio.coerceIn(0f, 1f)
    val inverseRatio = 1f - r

    // 1. 各チャンネルを抽出
    val a1 = (this shr 24 and 0xFF)
    val r1 = (this shr 16 and 0xFF)
    val g1 = (this shr 8 and 0xFF)
    val b1 = (this and 0xFF)

    val a2 = (color shr 24 and 0xFF)
    val r2 = (color shr 16 and 0xFF)
    val g2 = (color shr 8 and 0xFF)
    val b2 = (color and 0xFF)

    // 2. 線形補間を計算 (四捨五入のために 0.5f を加算)
    val a = (a1 * inverseRatio + a2 * r + 0.5f).toInt()
    val rResult = (r1 * inverseRatio + r2 * r + 0.5f).toInt()
    val g = (g1 * inverseRatio + g2 * r + 0.5f).toInt()
    val b = (b1 * inverseRatio + b2 * r + 0.5f).toInt()

    // 3. 結合して返す
    return (a shl 24) or (rResult shl 16) or (g shl 8) or b
}
