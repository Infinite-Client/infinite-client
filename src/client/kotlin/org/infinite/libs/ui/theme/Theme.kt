package org.infinite.libs.ui.theme

import org.infinite.libs.graphics.Graphics2D
import org.infinite.utils.alpha

abstract class Theme {
    open val colorScheme = ColorScheme()
    open fun renderBackGround(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        graphics2D: Graphics2D,
        alpha: Float = 1.0f,
    ) {
        val backgroundColor = colorScheme.backgroundColor
        graphics2D.fillStyle = backgroundColor.alpha((255 * alpha).toInt())
        graphics2D.fillRect(x, y, width, height)
    }

    fun renderBackGround(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        graphics2D: Graphics2D,
        alpha: Float = 1.0f,
    ) = renderBackGround(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), graphics2D, alpha)

    fun renderBackgroundWithRatio(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        graphics2D: Graphics2D,
        alpha: Float = 1.0f,
        ratio: Float = 0.5f,
    ) {
        val (xAlpha, yAlpha) = calculateMixedAlphas(alpha, ratio)
        val backgroundColor = colorScheme.backgroundColor
        graphics2D.fillStyle = backgroundColor.alpha((255 * xAlpha).toInt())
        graphics2D.fillRect(x, y, width, height)
        renderBackGround(x, y, width, height, graphics2D, yAlpha)
    }

    /**
     * 2つの背景の不透明度を計算する
     * @param alpha 最終的に実現したい合計の不透明度 (0.0f ~ 1.0f)
     * @param ratio 1枚目と2枚目のブレンド比率 (0.0f ~ 1.0f)
     * 1.0f のとき：(alpha, 0.0f) -> 1枚目のみ
     * 0.5f のとき：2枚を重ねて合計 alpha になるバランス
     * 0.0f のとき：(0.0f, alpha) -> 2枚目のみ
     * @return 1枚目と2枚目の不透明度のペア
     */
    private fun calculateMixedAlphas(alpha: Float, ratio: Float): Pair<Float, Float> {
        val a = alpha.coerceIn(0f, 1f)
        val r = ratio.coerceIn(0f, 1f)

        return when {
            r >= 1.0f -> Pair(a, 0.0f)

            r <= 0.0f -> Pair(0.0f, a)

            a <= 0.0f -> Pair(0.0f, 0.0f)

            else -> {
                // 最終的な透明度 a の中で、2枚目が占めるべき「色の割合」を計算
                // targetYAlpha は、最終的な色のうち「2枚目の色が占める不透明度」
                val targetYAlpha = a * (1.0f - r)

                // 2枚目の実際の不透明度 y
                val y = targetYAlpha

                // 1枚目の不透明度 x の逆算
                // 公式: a = x * (1 - y) + y  => x = (a - y) / (1 - y)
                val x = if (y < 1.0f) {
                    (a - y) / (1.0f - y)
                } else {
                    1.0f
                }

                Pair(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
            }
        }
    }
}
