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
        val uiScale = ultraUiFeature.effectiveUiScale(graphics2D)

        renderTime += graphics2D.delta * 0.05f
        val actualExp = player.experienceProgress.coerceIn(0f, 1f)
        animatedExp += (actualExp - animatedExp) * 0.1f

        val barWidth = 182f
        val barHeight = 4f
        val (x, y) = ultraUiFeature.topBoxOrigin(graphics2D)
        val padding = ultraUiFeature.padding.value.toFloat()
        val yOffset = if (containerUtil.isEnabled() && containerUtil.hotbarRotate.value) 64f else 0f

        graphics2D.push()
        graphics2D.translate(x, y)
        graphics2D.scale(uiScale, uiScale)

        if (containerUtil.isEnabled() && containerUtil.hotbarRotate.value) {
            val slotSize = 20f
            val hotbarWidth = 182f
            val baseY = padding + 2f + yOffset
            val frameHeight = 60f
            val frameX = 0f
            val frameY = baseY - frameHeight

            theme.renderBackGround(frameX, frameY, hotbarWidth, frameHeight, graphics2D, alphaValue * 0.8f)

            graphics2D.strokeStyle.width = 1f
            graphics2D.strokeStyle.color = colorScheme.accentColor.alpha((180 * alphaValue).toInt())
            graphics2D.strokeRect(frameX, frameY, hotbarWidth, frameHeight)

            for (i in 1 until 9) {
                val lineX = frameX + (i * slotSize) + 1f
                graphics2D.fillStyle = colorScheme.accentColor.alpha((40 * alphaValue).toInt())
                graphics2D.fillRect(lineX, frameY, 1f, frameHeight)
            }

            for (row in 1..2) {
                val lineY = frameY + (row * 20f)
                graphics2D.fillStyle = colorScheme.accentColor.alpha((30 * alphaValue).toInt())
                graphics2D.fillRect(frameX, lineY, hotbarWidth, 1f)
            }
        }
        graphics2D.fillStyle = colorScheme.backgroundColor.alpha((150 * alphaValue).toInt())
        graphics2D.fillRect(0f, 0f, barWidth, barHeight)

        val sHue = 90f // 黄緑
        val eHue = 160f // エメラルド
        graphics2D.renderLayeredBar(
            0f, 0f, barWidth, barHeight, animatedExp, actualExp,
            colorScheme.color(sHue, 0.8f, 0.6f), colorScheme.color(eHue, 0.8f, 0.6f),
            alphaValue,
            isRightToLeft = false,
            whiteColor = colorScheme.whiteColor,
            blackColor = colorScheme.blackColor,
        )

        if (player.experienceLevel > 0) {
            val levelText = player.experienceLevel.toString()
            graphics2D.textStyle.size = 10.0f
            graphics2D.textStyle.font = "infinite_regular"
            graphics2D.textStyle.shadow = true

            val textX = barWidth / 2f
            val textY = -6f

            val glow = (sin(renderTime * 3f) * 0.1f + 0.9f)
            graphics2D.fillStyle = colorScheme.color(sHue, 0.4f, glow).alpha((255 * alphaValue).toInt())
            graphics2D.textCentered(levelText, textX, textY)
        }

        graphics2D.pop()
    }
}
