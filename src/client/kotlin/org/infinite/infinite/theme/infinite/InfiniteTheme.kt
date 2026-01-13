package org.infinite.infinite.theme.infinite

import net.minecraft.client.Minecraft
import org.infinite.libs.graphics.Graphics2D
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
    private val loopTime = 5000.0

    override fun renderBackGround(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        graphics2D: Graphics2D,
        alpha: Float,
    ) {
        // 1. もとの場所(x, y)でクリップ範囲を有効にする
        // 注意: graphics2Dの scissor が絶対座標系を期待している場合、
        // push/setTransform の前に行うのが一般的です。
        graphics2D.enableScissor(x.toInt(), y.toInt(), width.toInt(), height.toInt())

        // 2. 状態を保存し、Transformをリセット（画面の絶対座標系へ）
        graphics2D.push()
        graphics2D.setTransform(
            1.0f,
            0.0f, // m00, m10 (Xのスケール, Xの傾き)
            0.0f,
            1.0f, // m01, m11 (Yの傾き, Yのスケール)
            0.0f,
            0.0f, // m02, m12 (Xの移動, Yの移動)
        )

        // 3. ウィンドウサイズから画面の中央（絶対座標）を取得
        val window = Minecraft.getInstance().window
        val screenW = window.guiScaledWidth.toFloat()
        val screenH = window.guiScaledHeight.toFloat()
        val centerX = screenW / 2f
        val centerY = screenH / 2f

        // 4. アニメーション計算
        val t = (System.currentTimeMillis() % loopTime.toLong()) / loopTime
        val baseColors = arrayOf(
            colorScheme.redColor,
            colorScheme.yellowColor,
            colorScheme.greenColor,
            colorScheme.cyanColor,
            colorScheme.blueColor,
            colorScheme.magentaColor,
        )

        val alphaInt = (255 * alpha).toInt()
        val centerColor = colorScheme.blackColor.alpha(alphaInt)

        // 画面全体を覆う半径
        val r = sqrt(screenW.pow(2) + screenH.pow(2))
        val size = baseColors.size

        // 5. 描画 (この座標は画面左上 0,0 を基準とした絶対座標)
        for (i in 0 until size step 2) {
            val color1 = baseColors[i].alpha(alphaInt)
            val color2 = baseColors[(i + 1) % size].alpha(alphaInt)
            val color3 = baseColors[(i + 2) % size].alpha(alphaInt)

            val d1 = 2.0 * PI * ((i.toDouble() / size + t) % 1.0)
            val d2 = 2.0 * PI * (((i + 1).toDouble() / size + t) % 1.0)
            val d3 = 2.0 * PI * (((i + 2).toDouble() / size + t) % 1.0)

            val x1 = centerX + r * cos(d1).toFloat()
            val y1 = centerY + r * sin(d1).toFloat()
            val x2 = centerX + r * cos(d2).toFloat()
            val y2 = centerY + r * sin(d2).toFloat()
            val x3 = centerX + r * cos(d3).toFloat()
            val y3 = centerY + r * sin(d3).toFloat()

            graphics2D.fillQuad(
                x1, y1, x2, y2, x3, y3, centerX, centerY,
                color1, color2, color3, centerColor,
            )
        }

        // 6. 状態を復元し、Scissorを解除
        graphics2D.pop()
        graphics2D.disableScissor()
    }
}
