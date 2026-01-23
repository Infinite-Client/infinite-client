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

class RightBoxRenderer :
    MinecraftInterface(),
    IUiRenderer {
    private val ultraUiFeature: UltraUiFeature
        get() = InfiniteClient.localFeatures.rendering.ultraUiFeature

    private var animatedFood = 0f
    private var animatedVehicle = 0f
    private var animatedAir = 0f

    // 幅のアニメーション用
    private var animatedWidthFactor = 1.0f

    private fun updateAnimation(): Triple<Float, Float, Float> {
        val player = player ?: return Triple(0f, 0f, 0f)

        val actualFood = (player.foodData.foodLevel / 20f).coerceIn(0f, 1f)
        val vehicle = player.vehicle
        val actualVehicle = if (vehicle is LivingEntity) (vehicle.health / vehicle.maxHealth).coerceIn(0f, 1f) else 0f
        val actualAir = (player.airSupply.toFloat() / player.maxAirSupply.toFloat()).coerceIn(0f, 1f)

        animatedFood += (actualFood - animatedFood) * 0.1f
        animatedVehicle += (actualVehicle - animatedVehicle) * 0.1f
        animatedAir += (actualAir - animatedAir) * 0.1f

        // オフハンド所持かつ利き手が左（オフハンドが右）の場合、幅を狭める
        val isOffhandOnRight = player.mainArm == HumanoidArm.LEFT && !player.offhandItem.isEmpty
        val targetFactor = if (isOffhandOnRight) 0.85f else 1.0f
        animatedWidthFactor += (targetFactor - animatedWidthFactor) * 0.5f

        return Triple(actualFood, actualVehicle, actualAir)
    }

    override fun render(graphics2D: Graphics2D) {
        val colorScheme = InfiniteClient.theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value

        val bH = ultraUiFeature.barHeight.value.toFloat()
        // アニメーション後の幅を計算
        val sM = ultraUiFeature.sideMargin.toFloat() * animatedWidthFactor
        val bottomY = graphics2D.height.toFloat()
        val startX = graphics2D.width.toFloat()

        val (actualFood, actualVehicle, actualAir) = updateAnimation()

        graphics2D.renderUltraBar(
            startX,
            bottomY - bH,
            sM,
            bH,
            1f,
            1f,
            colorScheme.backgroundColor,
            isRightToLeft = true,
        )

        val innerPadding = ultraUiFeature.padding.value.toFloat()
        val cH = bH - innerPadding
        val cW = sM - innerPadding
        val sat = 0.8f
        val bri = 0.5f

        fun draw(h: Float, cur: Float, tar: Float, sH: Float, eH: Float) {
            graphics2D.renderLayeredBar(
                startX, bottomY - h, cW, h, cur, tar,
                colorScheme.color(sH, sat, bri), colorScheme.color(eH, sat, bri),
                alphaValue, true, colorScheme.whiteColor, colorScheme.blackColor,
            )
        }

        draw(cH, animatedFood, actualFood, 60f, 120f)
        if (actualVehicle > 0 || animatedVehicle > 0) draw(cH * 0.5f, animatedVehicle, actualVehicle, 30f, 90f)
        if (actualAir < 1f || animatedAir > 0.01f) draw(cH * 0.4f, animatedAir, actualAir, 180f, 240f)
    }
}
