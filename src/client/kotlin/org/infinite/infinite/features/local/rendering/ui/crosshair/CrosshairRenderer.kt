package org.infinite.infinite.features.local.rendering.ui.crosshair

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.Items
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.rendering.ui.IUiRenderer
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.utils.alpha
import kotlin.math.PI

class CrosshairRenderer :
    MinecraftInterface(),
    IUiRenderer {

    private val ultraUiFeature: UltraUiFeature
        get() = InfiniteClient.localFeatures.rendering.ultraUiFeature

    private var rotationAnim = 0f
    private var lastAttackStrength = 1f
    private var smoothEntityFactor = 0f

    override fun render(graphics2D: Graphics2D) {
        val player = player ?: return
        val mc = minecraft
        val colorScheme = InfiniteClient.theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value
        val partialTicks = graphics2D.gameDelta

        val centerX = graphics2D.width / 2f
        val centerY = graphics2D.height / 2f

        // 1. ターゲット解析
        val hit = mc.hitResult
        val isEntity = hit?.type == HitResult.Type.ENTITY && (hit as? EntityHitResult)?.entity is LivingEntity
        val isBlock = hit?.type == HitResult.Type.BLOCK
        smoothEntityFactor += ((if (isEntity) 1f else 0f) - smoothEntityFactor) * 0.2f

        // 2. アニメーション計算 (攻撃)
        val attackStrength = player.getAttackStrengthScale(0f)
        if (attackStrength < lastAttackStrength && lastAttackStrength >= 0.99f) {
            rotationAnim = 90f
        }
        lastAttackStrength = attackStrength
        rotationAnim += (0f - rotationAnim) * 0.15f

        val useProgress = if (player.isUsingItem) {
            val currentTicks = player.ticksUsingItem - partialTicks
            val totalTicks = when (player.useItem.item) {
                Items.BOW -> 20

                // 弓: 1.0秒でフルチャージ
                Items.TRIDENT -> 10

                // トライデント: 0.5秒でフルチャージ
                Items.CROSSBOW -> {
                    // クロスボウはクイックチャージのエンチャントで速度が変わる
                    CrossbowItem.getChargeDuration(player.useItem, player)
                }

                Items.SPYGLASS -> 1

                // 望遠鏡: 即座（または非常に短い）
                Items.GOAT_HORN -> 140

                // ヤギの角笛: 約7秒（吹く時間）

                // 食料、ポーション、牛乳などは getUseDuration が 32 (1.6秒)
                // ただし Honey Bottle (40) や Dried Kelp (16) のような例外があるため
                // 72000 未満のものはそのまま getUseDuration を使うのが安全です
                else -> {
                    val duration = player.useItem.getUseDuration(player)
                    if (duration >= 72000) 32 else duration
                }
            }
            (currentTicks / totalTicks.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        // 描画開始
        graphics2D.push()
        graphics2D.translate(centerX, centerY)

        // 全体のアニメーション
        val finalScale = 1.0f + (smoothEntityFactor * 0.1f)
        graphics2D.scale(finalScale, finalScale)

        // 四方のラインとドットは回転させる
        graphics2D.push()
        graphics2D.rotateDegrees(rotationAnim + (smoothEntityFactor * 45f))

        val mainColor = if (smoothEntityFactor > 0.5f) colorScheme.accentColor else colorScheme.foregroundColor
        val shadowColor = colorScheme.backgroundColor.alpha((160 * alphaValue).toInt())

        fun fillWithThinOutline(x: Float, y: Float, w: Float, h: Float, color: Int) {
            val spread = 0.75f
            graphics2D.fillStyle = shadowColor
            graphics2D.fillRect(x - spread, y - spread, w + spread * 2, h + spread * 2)
            graphics2D.fillStyle = color.alpha((255 * alphaValue).toInt())
            graphics2D.fillRect(x, y, w, h)
        }

        // A. 中心ドット
        val dotSize = if (isBlock) 2.0f else 1.0f
        fillWithThinOutline(-dotSize / 2f, -dotSize / 2f, dotSize, dotSize, mainColor)

        // C. 四方のライン
        val gap = 2.2f + (1f - attackStrength) * 2.5f
        val thick = 0.8f
        val len = 2.2f
        fillWithThinOutline(-thick / 2f, -gap - len, thick, len, mainColor)
        fillWithThinOutline(-thick / 2f, gap, thick, len, mainColor)
        fillWithThinOutline(-gap - len, -thick / 2f, len, thick, mainColor)
        fillWithThinOutline(gap, -thick / 2f, len, thick, mainColor)

        graphics2D.pop() // 回転コンテキストを終了

        // B. 円形ゲージ (回転の影響を受けないように個別に制御)
        val progress = if (useProgress > 0f) useProgress else attackStrength
        if (progress < 0.99f) {
            val radius = 10.0f
            val startAngle = -PI.toFloat() / 2f
            val sweepAngle = 2 * PI.toFloat() * progress

            // アウトライン (背面に全円を描画)
            graphics2D.strokeStyle.width = 1.6f
            graphics2D.strokeStyle.color = shadowColor
            graphics2D.beginPath()
            graphics2D.arc(0f, 0f, radius, 0f, (2 * PI).toFloat())
            graphics2D.strokePath()

            // メインゲージ
            graphics2D.strokeStyle.width = 0.8f
            graphics2D.strokeStyle.color = mainColor.alpha((255 * alphaValue).toInt())
            graphics2D.beginPath()
            graphics2D.arc(0f, 0f, radius, startAngle, startAngle + sweepAngle)
            graphics2D.strokePath()
        }

        graphics2D.pop() // 全体の translate/scale を終了
    }
}
