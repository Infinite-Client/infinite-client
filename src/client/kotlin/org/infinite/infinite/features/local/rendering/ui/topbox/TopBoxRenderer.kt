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
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value

        // 1. アニメーション更新
        renderTime += graphics2D.gameDelta * 0.05f
        val actualExp = player.experienceProgress.coerceIn(0f, 1f)
        animatedExp += (actualExp - animatedExp) * 0.1f

        // 2. レイアウト計算 (ホットバーの少し上)
        val barWidth = 182f
        val barHeight = 6f
        val x = (graphics2D.width - barWidth) / 2f
        val y = graphics2D.height - ultraUiFeature.barHeight.value.toFloat() - barHeight - 4f

        graphics2D.push()

        // 3. 微細な浮遊アニメーション (上下にゆっくり揺れる)
        val floatOffset = sin(renderTime) * 0.5f
        graphics2D.translate(0f, floatOffset)

        // 4. 背景の描画 (斜めカットデザイン)
        // theme.renderBackGround をベースにしつつ、上部に配置
        theme.renderBackGround(x, y, barWidth, barHeight, graphics2D, alphaValue)

        // 5. 経験値バー本体 (平行四辺形風に描画)
        // ライムグリーン(90f) -> シアン(180f) へのグラデーション
        val sHue = 90f
        val eHue = 180f
        val sat = 0.8f
        val bri = 0.7f

        // renderLayeredBar を使用して、増減アニメーションに対応
        graphics2D.renderLayeredBar(
            x, y, barWidth, barHeight, animatedExp, actualExp,
            colorScheme.color(sHue, sat, bri), colorScheme.color(eHue, sat, bri),
            alphaValue,
            isRightToLeft = false,
            whiteColor = colorScheme.whiteColor,
            blackColor = colorScheme.blackColor,
        )

        // 6. 装飾的な枠線 (アクセントカラー)
        graphics2D.strokeStyle.width = 1f
        graphics2D.fillStyle = colorScheme.accentColor.alpha((200 * alphaValue).toInt())
        graphics2D.strokeRect(x, y, barWidth, barHeight)

        // 7. レベル表示 (テクニカルな配置)
        if (player.experienceLevel > 0) {
            val levelText = player.experienceLevel.toString()

            graphics2D.push()
            // わずかに傾ける (斜めのデザインに合わせる)
            graphics2D.translate(x + barWidth / 2f, y - 4f)
            graphics2D.rotateDegrees(sin(renderTime * 0.5f) * 2f) // 左右にわずかに揺れる

            graphics2D.textStyle.size = 0.9f
            graphics2D.textStyle.shadow = true

            // レベルが高いほど色が輝くように
            val glow = (sin(renderTime * 2f) * 0.2f + 0.8f)
            graphics2D.fillStyle = colorScheme.color(sHue, 0.4f, glow).alpha((255 * alphaValue).toInt())

            graphics2D.textCentered(levelText, 0f, 0f)
            graphics2D.pop()
        }

        // 8. 経験値が溜まっている時の「粒子」のような装飾 (オプション)
        if (actualExp > 0) {
            val particleX = x + (barWidth * animatedExp)
            graphics2D.fillStyle = colorScheme.whiteColor.alpha((150 * alphaValue).toInt())
            graphics2D.fillRect(particleX - 1f, y - 1f, 2f, barHeight + 2f)
        }

        graphics2D.pop()
    }
}
