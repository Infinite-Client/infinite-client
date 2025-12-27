package org.infinite.ultimate.theme.infinite

import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.ui.theme.ColorScheme
import org.infinite.libs.ui.theme.Theme
import org.infinite.utils.alpha
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class InfiniteTheme : Theme() {
    override val colorScheme: ColorScheme = InfiniteColorScheme()
    private val loopTime = 5000.0 // 5秒で一周

    override fun renderBackGround(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        graphics2DRenderer: Graphics2DRenderer,
        alpha: Float,
    ) {
        val centerX = x + width / 2f
        val centerY = y + height / 2f

        // 現在の時間に基づくオフセット (0.0 ~ 1.0)
        val t = (System.currentTimeMillis() % loopTime.toLong()) / loopTime

        val baseColors = arrayOf(
            colorScheme.redColor,
            colorScheme.yellowColor,
            colorScheme.greenColor,
            colorScheme.aquaColor,
            colorScheme.blueColor,
            colorScheme.magentaColor,
        )

        val alphaInt = (255 * alpha).toInt()
        val centerColor = colorScheme.blackColor.alpha(alphaInt)

        // 画面全体を覆うのに十分な半径
        val r = sqrt(width.pow(2) + height.pow(2)) * (1 + 4 * alpha)
        val size = baseColors.size

        graphics2DRenderer.enableScissor(x.toInt(), y.toInt(), width.toInt(), height.toInt())

// ... 前後の処理は同じ ...

        // ループを2ステップずつ進める
        for (i in 0 until size step 2) {
            val color1 = baseColors[i].alpha(alphaInt)
            val color2 = baseColors[(i + 1) % size].alpha(alphaInt)
            val color3 = baseColors[(i + 2) % size].alpha(alphaInt)

            // 3つの外周ポイントの角度
            val d1 = 2.0 * PI * ((i.toDouble() / size + t) % 1.0)
            val d2 = 2.0 * PI * (((i + 1).toDouble() / size + t) % 1.0)
            val d3 = 2.0 * PI * (((i + 2).toDouble() / size + t) % 1.0)

            // 座標計算
            val x1 = centerX + r * cos(d1).toFloat()
            val y1 = centerY + r * sin(d1).toFloat()
            val x2 = centerX + r * cos(d2).toFloat()
            val y2 = centerY + r * sin(d2).toFloat()
            val x3 = centerX + r * cos(d3).toFloat()
            val y3 = centerY + r * sin(d3).toFloat()

            /*
             * fillQuad の頂点指定:
             * 多くのライブラリでは 1(x1,y1) -> 2(x2,y2) -> 3(x3,y3) -> 4(centerX,centerY)
             * のように反時計回りまたは時計回りに指定します。
             */
            graphics2DRenderer.fillQuad(
                x1, y1, x2, y2, x3, y3, centerX, centerY,
                color1, color2, color3, centerColor,
            )
        }

        graphics2DRenderer.disableScissor()
        graphics2DRenderer.flush()
    }
}
