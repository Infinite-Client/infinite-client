package org.infinite.infinite.features.local.rendering.ui.right

import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.rendering.ui.IUiRenderer
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature.Companion.renderLayeredBar
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature.Companion.renderUltraBar
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.utils.Quadruple
import org.infinite.utils.alpha
import org.infinite.utils.mix

class RightBoxRenderer :
    MinecraftInterface(),
    IUiRenderer {
    private val ultraUiFeature: UltraUiFeature
        get() = InfiniteClient.localFeatures.rendering.ultraUiFeature

    private var animatedFood = 0f
    private var animatedSaturation = 0f
    private var animatedVehicle = 0f
    private var animatedAir = 0f

    // 幅のアニメーション用
    private var animatedWidthFactor = 1.0f

    private fun updateAnimation(): Quadruple<Float, Float, Float, Float> {
        val player = player ?: return Quadruple(0f, 0f, 0f, 0f)

        val actualFood = (player.foodData.foodLevel / 20f).coerceIn(0f, 1f)
        val actualSaturation = (player.foodData.saturationLevel / 20f).coerceIn(0f, 1f)
        val vehicle = player.vehicle
        val actualVehicle = if (vehicle is LivingEntity) (vehicle.health / vehicle.maxHealth).coerceIn(0f, 1f) else 0f
        val actualAir = (player.airSupply.toFloat() / player.maxAirSupply.toFloat()).coerceIn(0f, 1f)
        val animateSpeed = 0.2f
        animatedFood += (actualFood - animatedFood) * animateSpeed
        animatedSaturation += (actualSaturation - animatedSaturation) * animateSpeed
        animatedVehicle += (actualVehicle - animatedVehicle) * animateSpeed
        animatedAir += (actualAir - animatedAir) * animateSpeed

        // オフハンド所持かつ利き手が左（オフハンドが右）の場合、幅を狭める
        val isOffhandOnRight = player.mainArm == HumanoidArm.LEFT && !player.offhandItem.isEmpty
        val targetFactor = if (isOffhandOnRight) 0.85f else 1.0f
        animatedWidthFactor += (targetFactor - animatedWidthFactor) * 0.5f

        return Quadruple(actualFood, actualSaturation, actualVehicle, actualAir)
    }

    override fun render(graphics2D: Graphics2D) {
        val colorScheme = InfiniteClient.theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value

        val bH = ultraUiFeature.barHeight.value.toFloat()
        // アニメーション後の幅を計算
        val sM = ultraUiFeature.sideMargin.toFloat() * animatedWidthFactor
        val bottomY = graphics2D.height.toFloat()
        val startX = graphics2D.width.toFloat()

        val (actualFood, actualSaturation, actualVehicle, actualAir) = updateAnimation()
        val baseColor =
            colorScheme.backgroundColor.mix(colorScheme.accentColor, 0.1f)
        val baseAlpha = ultraUiFeature.alpha.value
        graphics2D.renderUltraBar(
            startX,
            bottomY - bH,
            sM,
            bH,
            1f,
            1f,
            baseColor,
            isRightToLeft = true,
        )

        val innerPadding = ultraUiFeature.padding.value.toFloat()
        val cH = bH - innerPadding
        val cW = sM - innerPadding
        val sat = 0.8f
        val bri = 0.5f

        fun draw(h: Float, cur: Float, tar: Float, sH: Float, eH: Float) {
            graphics2D.renderLayeredBar(
                startX,
                bottomY - h,
                cW,
                h,
                cur,
                tar,
                colorScheme.color(sH, sat, bri).alpha((255 * baseAlpha).toInt()),
                colorScheme.color(eH, sat, bri).alpha((255 * baseAlpha).toInt()),
                alphaValue,
                true,
                colorScheme.whiteColor,
                colorScheme.blackColor,
            )
        }

        draw(cH, animatedFood, actualFood, 45f, 75f)
        draw(cH, animatedSaturation, actualSaturation, 60f, 90f)
        if (actualVehicle > 0 || animatedVehicle > 0) draw(cH * 0.6f, animatedVehicle, actualVehicle, 30f, 90f)
        if (actualAir < 1f || animatedAir > 0.01f) {
            // 1.0 (100%) で 0, 0.8 (80%) 以下で 1.0 (255) になるよう計算
            // (1.0 - actualAir) / 0.2f により、2割削れた時点で 1.0 に到達する
            val airAlphaFactor = ((1f - actualAir) / 0.2f).coerceIn(0f, 1f)
            val dynamicAlpha = alphaValue * airAlphaFactor * baseAlpha

            // colorScheme.color() で得た Int 型の色に対し .alpha((255 * dynamicAlpha).toInt()) を適用
            val startColor = colorScheme.color(180f, sat, bri).alpha((255 * dynamicAlpha).toInt())
            val endColor = colorScheme.color(240f, sat, bri).alpha((255 * dynamicAlpha).toInt())

            graphics2D.renderLayeredBar(
                startX, bottomY - (cH * 0.3f), cW, cH * 0.3f, animatedAir, actualAir,
                startColor, endColor,
                dynamicAlpha, true, colorScheme.whiteColor, colorScheme.blackColor,
            )
        }
    }
}
