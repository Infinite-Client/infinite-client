package org.infinite.infinite.features.local.rendering.ui.topbox

import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.inventory.control.ContainerUtilFeature
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

    private val containerUtil: ContainerUtilFeature
        get() = InfiniteClient.localFeatures.inventory.containerUtilFeature

    private var animatedExp = 0f
    private var renderTime = 0f

    override fun render(graphics2D: Graphics2D) {
        val player = player ?: return
        val colorScheme = InfiniteClient.theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value
        val theme = InfiniteClient.theme

        // --- 1. アニメーション更新 ---
        renderTime += graphics2D.gameDelta * 0.05f
        val actualExp = player.experienceProgress.coerceIn(0f, 1f)
        animatedExp += (actualExp - animatedExp) * 0.1f

        // --- 2. レイアウト計算 ---
        val barWidth = 182f
        val barHeight = 4f
        val x = (graphics2D.width - barWidth) / 2f
        val padding = ultraUiFeature.padding.value.toFloat()

        // 経験値バーのY座標を、枠がある時だけさらに上に避ける
        // 経験値バーを枠のさらに 4px 上に配置
        val yOffset = if (containerUtil.isEnabled() && containerUtil.hotbarRotate.value) 64f else 0f
        val y = graphics2D.height - 20f - padding - barHeight - 4f - yOffset
        // --- 3. ContainerUtil 用のプレビュー枠描画 ---
        if (containerUtil.isEnabled() && containerUtil.hotbarRotate.value) {
            val slotSize = 20f
            val hotbarWidth = 182f

            // 底辺の基準 (ContainerUtilFeature の baseY と同じ)
            val baseY = graphics2D.height - 22f - barHeight

            // アイテムは最大で 3段分 (row=2, distance≒3) 上に描画されるため
            // 20px * 3段 = 60px 分の高さが必要。
            val frameHeight = 60f
            val frameX = (graphics2D.width - hotbarWidth) / 2f

            // frameY は「底辺から高さを引いた位置」にする
            val frameY = baseY - frameHeight

            // 1. 背面のパネル
            theme.renderBackGround(frameX, frameY, hotbarWidth, frameHeight, graphics2D, alphaValue * 0.8f)

            // 2. 外枠
            graphics2D.strokeStyle.width = 1f
            graphics2D.strokeStyle.color = colorScheme.accentColor.alpha((180 * alphaValue).toInt())
            graphics2D.strokeRect(frameX, frameY, hotbarWidth, frameHeight)

            // 3. 縦の区切り線
            for (i in 1 until 9) {
                val lineX = frameX + (i * slotSize) + 1f
                graphics2D.fillStyle = colorScheme.accentColor.alpha((40 * alphaValue).toInt())
                graphics2D.fillRect(lineX, frameY, 1f, frameHeight)
            }

            // 4. 横の区切り線 (上から 20px, 40px の位置)
            for (row in 1..2) {
                val lineY = frameY + (row * 20f)
                graphics2D.fillStyle = colorScheme.accentColor.alpha((30 * alphaValue).toInt())
                graphics2D.fillRect(frameX, lineY, hotbarWidth, 1f)
            }
        }
        // --- 4. 経験値バー背景 ---
        graphics2D.fillStyle = colorScheme.backgroundColor.alpha((150 * alphaValue).toInt())
        graphics2D.fillRect(x, y, barWidth, barHeight)

        // --- 5. 経験値バー本体 ---
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

        // --- 6. レベル数字 ---
        if (player.experienceLevel > 0) {
            val levelText = player.experienceLevel.toString()
            graphics2D.textStyle.size = 10.0f
            graphics2D.textStyle.font = "infinite_regular"
            graphics2D.textStyle.shadow = true

            val textX = x + barWidth / 2f
            val textY = y - 6f // バーの直上に配置

            val glow = (sin(renderTime * 3f) * 0.1f + 0.9f)
            graphics2D.fillStyle = colorScheme.color(sHue, 0.4f, glow).alpha((255 * alphaValue).toInt())
            graphics2D.textCentered(levelText, textX, textY)
        }
    }
}
