package org.infinite.libs.ui.theme

open class ColorScheme {
    enum class SchemeType {
        Dark, Light
    }

    open val backgroundColor: Int
        get() = when (schemeType) {
            SchemeType.Dark -> color(0f, 0f, 0.1f, 1f)
            SchemeType.Light -> color(0f, 0f, 1f, 1f)
        }
    open val foregroundColor: Int
        get() = when (schemeType) {
            SchemeType.Light -> color(0f, 0f, 0.1f, 1f)
            SchemeType.Dark -> color(0f, 0f, 1f, 1f)
        }
    open val surfaceColor: Int
        get() = when (schemeType) {
            SchemeType.Dark -> color(0f, 0f, 0.15f, 1f) // 少し明るい黒
            SchemeType.Light -> color(0f, 0f, 0.95f, 1f) // 少し暗い白
        }

    // 選択状態を示すアクセントカラー（例: 青ベース）
    open val accentColor: Int
        get() = blueColor // または color(210f, 0.8f, 0.6f, 1f)

    // 非活性（Disabled）なテキスト
    open val secondaryTextColor: Int
        get() = color(0f, 0f, 0.5f, 1f) // 中間グレー

    // --- 追加: 状態変化用ヘルパー ---

    /** 指定した色をマウスホバー用に少し明るく/強調する */
    fun getHoverColor(baseColor: Int): Int {
        return interpolate(baseColor, whiteColor, 0.2f)
    }

    open val schemeType = SchemeType.Dark
    open val redColor: Int = 0xFFFF0000.toInt()
    open val yellowColor: Int = 0xFFFFFF00.toInt()
    open val greenColor: Int = 0xFF00FF00.toInt()
    open val aquaColor: Int = 0xFF00FFFF.toInt()
    open val blueColor: Int = 0xFF0000FF.toInt()
    open val magentaColor: Int = 0xFFFF00FF.toInt()
    open val whiteColor: Int = 0xFFFFFFFF.toInt()
    open val blackColor: Int = 0xFF000000.toInt()
    fun color(hue: Float, saturation: Float, lightness: Float, alpha: Float): Int {
        if (saturation <= 0.001f) {
            return interpolate(blackColor, whiteColor, lightness, alpha)
        }

        // 2. 有彩色の場合: 色相(hue)に基づいて、隣り合う2つの基本色を特定して混ぜる
        // (例: 0-60度なら Red と Yellow の間)
        val h = (hue % 360f + 360f) % 360f
        val sector = h / 60f
        val fraction = sector - sector.toInt()

        val baseColor = when (sector.toInt()) {
            0 -> interpolate(redColor, yellowColor, fraction)
            1 -> interpolate(yellowColor, greenColor, fraction)
            2 -> interpolate(greenColor, aquaColor, fraction)
            3 -> interpolate(aquaColor, blueColor, fraction)
            4 -> interpolate(blueColor, magentaColor, fraction)
            else -> interpolate(magentaColor, redColor, fraction)
        }

        // 3. 彩度(saturation)に応じて、無彩色(グレー)から有彩色へ混ぜる
        val gray = interpolate(blackColor, whiteColor, lightness)
        val saturatedColor = adjustLightness(baseColor, lightness) // 輝度調整

        return interpolate(gray, saturatedColor, saturation, alpha)
    }

    /** 2つのARGB色を 0.0~1.0 の比率で線形補間する */
    private fun interpolate(c1: Int, c2: Int, fraction: Float, alphaOverride: Float? = null): Int {
        val a1 = (c1 shr 24) and 0xFF
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF

        val a2 = (c2 shr 24) and 0xFF
        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF

        val resA = alphaOverride?.let { (it * 255).toInt() } ?: (a1 + (a2 - a1) * fraction).toInt()
        val resR = (r1 + (r2 - r1) * fraction).toInt()
        val resG = (g1 + (g2 - g1) * fraction).toInt()
        val resB = (b1 + (b2 - b1) * fraction).toInt()

        return (resA shl 24) or (resR shl 16) or (resG shl 8) or resB
    }

    private fun adjustLightness(color: Int, l: Float): Int {
        return if (l < 0.5f) {
            interpolate(blackColor, color, l * 2f)
        } else {
            interpolate(color, whiteColor, (l - 0.5f) * 2f)
        }
    }
}
