package org.infinite.infinite.features.local.rendering.ui.topbox

import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.rendering.ui.IUiRenderer
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature.Companion.renderLayeredBar
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.utils.alpha
import kotlin.math.sin

class TopBoxRenderer :
    MinecraftInterface(),
    IUiRenderer {
    private val ultraUiFeature: UltraUiFeature
        get() = InfiniteClient.localFeatures.rendering.ultraUiFeature

    private var animatedExp = 0f
    private var renderTime = 0f

    override fun render(graphics2D: Graphics2D) {
        val player = player ?: return
        val colorScheme = InfiniteClient.theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value

        // 1. アニメーション更新
        renderTime += graphics2D.gameDelta * 0.05f
        val actualExp = player.experienceProgress.coerceIn(0f, 1f)
        animatedExp += (actualExp - animatedExp) * 0.1f

        // 2. レイアウト計算 (ホットバーの直上)
        val barWidth = 182f
        val barHeight = 4f
        val x = (graphics2D.width - barWidth) / 2f
        // y座標の計算: 画面下端 - サイドバー高さ - このバーの高さ - マージン
        val y = graphics2D.height - ultraUiFeature.barHeight.value.toFloat() - barHeight - 4f

        // 3. 経験値バー背景
        // theme.renderBackGround の代わりに直接 fillRect で背景を描画して確実に表示を確認
        graphics2D.fillStyle = colorScheme.backgroundColor.alpha((150 * alphaValue).toInt())
        graphics2D.fillRect(x, y, barWidth, barHeight)

        // 4. 経験値バー本体 (renderLayeredBarを使用)
        val sHue = 90f // 黄緑
        val eHue = 160f // エメラルド

        graphics2D.renderLayeredBar(
            x, y, barWidth, barHeight, animatedExp, actualExp,
            colorScheme.color(sHue, 0.8f, 0.6f), colorScheme.color(eHue, 0.8f, 0.6f),
            alphaValue,
            isRightToLeft = false,
            whiteColor = colorScheme.whiteColor,
            blackColor = colorScheme.blackColor,
        )

        // 5. レベル数字の描画
        if (player.experienceLevel > 0) {
            val levelText = player.experienceLevel.toString()
            graphics2D.textStyle.size = 10.0f

            graphics2D.textStyle.font = "infinite_regular"
            graphics2D.textStyle.shadow = true
            graphics2D.fillStyle = colorScheme.greenColor
            // 座標を translate なしで直接指定
            val textX = x + barWidth / 2f
            val textY = y - graphics2D.textStyle.size / 2f
            graphics2D.textStyle.shadow = true

            // 輝くような緑色を計算
            val glow = (sin(renderTime * 3f) * 0.1f + 0.9f)
            graphics2D.fillStyle = colorScheme.color(sHue, 0.4f, glow).alpha((255 * alphaValue).toInt())

            // 中央揃えで描画
            graphics2D.textCentered(levelText, textX, textY)
        }
    }
}
