package org.infinite.infinite.features.local.rendering.ui.crosshair

import net.minecraft.world.entity.LivingEntity
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
        val colorScheme = InfiniteClient.theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value

        val centerX = graphics2D.width / 2f
        val centerY = graphics2D.height / 2f

        // 1. ターゲット解析
        val hit = minecraft.hitResult
        val isEntity = hit?.type == HitResult.Type.ENTITY && (hit as? EntityHitResult)?.entity is LivingEntity
        val isBlock = hit?.type == HitResult.Type.BLOCK
        smoothEntityFactor += ((if (isEntity) 1f else 0f) - smoothEntityFactor) * 0.2f

        // 2. アニメーション計算
        val attackStrength = player.getAttackStrengthScale(0f)
        if (attackStrength < lastAttackStrength && lastAttackStrength >= 0.99f) {
            rotationAnim = 90f
        }
        lastAttackStrength = attackStrength
        rotationAnim += (0f - rotationAnim) * 0.15f

        graphics2D.push()
        graphics2D.translate(centerX, centerY)

        val finalScale = 1.0f + (smoothEntityFactor * 0.1f)
        graphics2D.scale(finalScale, finalScale)
        graphics2D.rotateDegrees(rotationAnim + (smoothEntityFactor * 45f))

        val mainColor = if (smoothEntityFactor > 0.5f) colorScheme.accentColor else colorScheme.foregroundColor
        val shadowColor = colorScheme.backgroundColor.alpha((160 * alphaValue).toInt())

        // --- 描画ユーティリティ ---
        fun fillWithThinOutline(x: Float, y: Float, w: Float, h: Float, color: Int) {
            val spread = 0.75f
            graphics2D.fillStyle = shadowColor
            graphics2D.fillRect(x - spread, y - spread, w + spread * 2, h + spread * 2)
            graphics2D.fillStyle = color.alpha((255 * alphaValue).toInt())
            graphics2D.fillRect(x, y, w, h)
        }

        // --- A. 中心ドット ---
        val dotSize = if (isBlock) 2.0f else 1.0f
        fillWithThinOutline(-dotSize / 2f, -dotSize / 2f, dotSize, dotSize, mainColor)

        // --- B. 円形ゲージ (修正ポイント) ---
        // 弓や食料などの「使用中」の進捗を計算
        val useProgress = if (player.isUsingItem) {
            val maxDur = player.useItem.getUseDuration(player).toFloat()
            if (maxDur > 0f) {
                // (最大時間 - 残り時間) / 最大時間 で 0.0 ~ 1.0 を取得
                ((maxDur - player.useItemRemainingTicks.toFloat()) / maxDur).coerceIn(0f, 1f)
            } else {
                0f
            }
        } else {
            0f
        }

        // 攻撃の溜め(attackStrength)か、アイテムの溜め(useProgress)のどちらかを使う
        // アイテム使用中ならそちらを優先。そうでないなら攻撃ゲージを表示。
        val progress = if (useProgress > 0f) useProgress else attackStrength
        // ゲージがフルでない時だけ描画
        if (progress < 0.99f) {
            graphics2D.push()
            graphics2D.rotateDegrees(-rotationAnim - (smoothEntityFactor * 45f))
            val radius = 10.0f
            // アウトライン
            graphics2D.strokeStyle.width = 1.6f
            graphics2D.strokeStyle.color = shadowColor
            graphics2D.beginPath()
            graphics2D.arc(0f, 0f, radius, 0f, (2 * PI).toFloat())
            graphics2D.strokePath()
            // メインゲージ
            graphics2D.strokeStyle.width = 0.8f
            graphics2D.strokeStyle.color = mainColor.alpha((255 * alphaValue).toInt())
            val startAngle = -PI.toFloat() / 2f
            val endAngle = startAngle + (2 * PI.toFloat() * progress)
            graphics2D.beginPath()
            graphics2D.arc(0f, 0f, radius, startAngle, endAngle)
            graphics2D.strokePath()
            graphics2D.pop()
        }
        // --- C. 四方のライン ---
        val gap = 2.2f + (1f - attackStrength) * 2.5f
        val thick = 0.8f
        val len = 2.2f

        fillWithThinOutline(-thick / 2f, -gap - len, thick, len, mainColor)
        fillWithThinOutline(-thick / 2f, gap, thick, len, mainColor)
        fillWithThinOutline(-gap - len, -thick / 2f, len, thick, mainColor)
        fillWithThinOutline(gap, -thick / 2f, len, thick, mainColor)

        graphics2D.pop()
    }
}
