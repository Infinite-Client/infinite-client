package org.infinite.infinite.features.local.rendering.ui.left

import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.ai.attributes.Attributes
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.rendering.ui.IUiRenderer
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature.Companion.renderLayeredBar
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature.Companion.renderUltraBar
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.utils.alpha

class LeftBoxRenderer :
    MinecraftInterface(),
    IUiRenderer {
    private val ultraUiFeature: UltraUiFeature
        get() = InfiniteClient.localFeatures.rendering.ultraUiFeature

    private var animatedHealth = 0f
    private var animatedArmor = 0f
    private var animatedToughness = 0f

    // 幅のアニメーション用
    private var animatedWidthFactor = 1.0f

    private fun updateAnimation(): Triple<Float, Float, Float> {
        val player = player ?: return Triple(0f, 0f, 0f)

        val actualHealth = (player.health / player.maxHealth).coerceIn(0f, 1f)
        val actualArmor = (player.armorValue / 20f).coerceIn(0f, 1f)
        val actualToughness =
            ((player.attributes.getInstance(Attributes.ARMOR_TOUGHNESS)?.value?.toFloat() ?: 0f) / 20f).coerceIn(0f, 1f)

        animatedHealth += (actualHealth - animatedHealth) * 0.1f
        animatedArmor += (actualArmor - animatedArmor) * 0.1f
        animatedToughness += (actualToughness - animatedToughness) * 0.1f

        // オフハンド所持かつ利き手が右（オフハンドが左）の場合、幅を狭める
        val isOffhandOnLeft = player.mainArm == HumanoidArm.RIGHT && !player.offhandItem.isEmpty
        val targetFactor = if (isOffhandOnLeft) 0.85f else 1.0f
        animatedWidthFactor += (targetFactor - animatedWidthFactor) * 0.5f

        return Triple(actualHealth, actualArmor, actualToughness)
    }

    override fun render(graphics2D: Graphics2D) {
        val colorScheme = InfiniteClient.theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value

        val bH = ultraUiFeature.barHeight.value.toFloat()
        // アニメーション後の幅を計算
        val sM = ultraUiFeature.sideMargin.toFloat() * animatedWidthFactor
        val bottomY = graphics2D.height.toFloat()
        val (actualHealth, actualArmor, actualToughness) = updateAnimation()
        val alphaInt = (alphaValue * 255).toInt()
        graphics2D.renderUltraBar(0f, bottomY - bH, sM, bH, 1f, 1f, colorScheme.backgroundColor.alpha(alphaInt))

        val innerPadding = ultraUiFeature.padding.value.toFloat()
        val cH = bH - innerPadding
        val cW = sM - innerPadding
        val sat = 0.8f
        val bri = 0.5f

        fun draw(h: Float, cur: Float, tar: Float, sH: Float, eH: Float) {
            graphics2D.renderLayeredBar(
                0f, bottomY - h, cW, h, cur, tar,
                colorScheme.color(sH, sat, bri), colorScheme.color(eH, sat, bri),
                alphaValue, false, colorScheme.whiteColor, colorScheme.blackColor,
            )
        }

        draw(cH, animatedHealth, actualHealth, 0f, 60f)
        if (actualArmor > 0 || animatedArmor > 0) draw(cH * 0.5f, animatedArmor, actualArmor, 120f, 180f)
        if (actualToughness > 0 || animatedToughness > 0) {
            draw(
                cH * 0.4f,
                animatedToughness,
                actualToughness,
                210f,
                270f,
            )
        }
    }
}
