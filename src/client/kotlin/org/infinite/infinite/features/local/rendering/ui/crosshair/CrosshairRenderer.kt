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
        val options = mc.options

        // 1. 描画座標の決定
        val (renderX, renderY) = if (options.cameraType.isFirstPerson) {
            // 一人称視点：画面中央
            graphics2D.width / 2f to graphics2D.height / 2f
        } else {
            // 三人称視点：レイキャストで衝突地点を計算
            val pickRange = 100.0 // レイキャストの最大距離
            val hitResult = player.pick(pickRange, graphics2D.gameDelta, false)
            val worldPos = hitResult.location

            // ワールド座標をスクリーン座標に投影
            val screenPos = graphics2D.projectWorldToScreen(worldPos) ?: return
            screenPos.first to screenPos.second
        }

        // 2. メインの描画処理を呼び出し
        renderCrosshair(graphics2D, renderX, renderY)
    }

    /**
     * 指定した座標にクロスヘアを描画する
     */
    fun renderCrosshair(graphics2D: Graphics2D, x: Float, y: Float) {
        val player = player ?: return
        val level = level ?: return
        val colorScheme = InfiniteClient.theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value
        val partialTicks = graphics2D.gameDelta

        // 1. ターゲット解析
        val hit = minecraft.hitResult
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

                Items.TRIDENT -> 10

                Items.CROSSBOW -> CrossbowItem.getChargeDuration(player.useItem, player)

                Items.SPYGLASS -> 1

                Items.GOAT_HORN -> 140

                else -> {
                    val duration = player.useItem.getUseDuration(player)
                    if (duration >= 72000) 32 else duration
                }
            }
            (currentTicks / totalTicks.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val breakProgress = minecraft.gameMode?.let {
            if (it.isDestroying) it.destroyProgress else 0f
        } ?: 0f // 描画開始
        graphics2D.push()
        graphics2D.translate(x, y) // 指定された座標へ移動

        // 全体のアニメーション
        val finalScale = 1.0f + (smoothEntityFactor * 0.1f)
        graphics2D.scale(finalScale, finalScale)

        // 四方のラインとドットは回転させる
        graphics2D.push()
        graphics2D.rotateDegrees(rotationAnim + (smoothEntityFactor * 45f))

        val mainColor = if (smoothEntityFactor > 0.5f) colorScheme.accentColor else colorScheme.foregroundColor
        val shadowColor = colorScheme.backgroundColor.alpha((160 * alphaValue).toInt())

        // 内部ヘルパー関数
        fun fillWithThinOutline(ox: Float, oy: Float, w: Float, h: Float, color: Int) {
            val spread = 0.75f
            graphics2D.fillStyle = shadowColor
            graphics2D.fillRect(ox - spread, oy - spread, w + spread * 2, h + spread * 2)
            graphics2D.fillStyle = color.alpha((255 * alphaValue).toInt())
            graphics2D.fillRect(ox, oy, w, h)
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

        // B. 円形ゲージ
        val progress = if (useProgress > 0f) useProgress else attackStrength
        val radius = 10.0f
        val color = mainColor.alpha((255 * alphaValue).toInt())
        if (progress < 0.99f) {
            val startAngle = -PI.toFloat() / 2f
            val sweepAngle = 2 * PI.toFloat() * progress

            graphics2D.strokeStyle.width = 1.6f
            graphics2D.strokeStyle.color = shadowColor
            graphics2D.beginPath()
            graphics2D.arc(0f, 0f, radius, 0f, (2 * PI).toFloat())
            graphics2D.strokePath()

            graphics2D.strokeStyle.width = 0.8f
            graphics2D.strokeStyle.color = color
            graphics2D.beginPath()
            graphics2D.arc(0f, 0f, radius, startAngle, startAngle + sweepAngle)
            graphics2D.strokePath()
        }
        val isBreaking = minecraft.gameMode?.isDestroying ?: false
        if (isBreaking) {
            // 【掘削中：正方形ゲージ】
            val size = radius * 1.2f
            graphics2D.strokeStyle.width = 1.6f
            graphics2D.strokeStyle.color = shadowColor
            graphics2D.strokeRect(-size, -size, size * 2, size * 2)

            graphics2D.strokeStyle.width = 1f
            graphics2D.strokeStyle.color = color
            val p = breakProgress.coerceIn(0f, 1f)
            graphics2D.beginPath()
            graphics2D.moveTo(0f, -size) // スタート地点：上辺中央
            graphics2D.lineTo(size * (p / 0.125f).coerceIn(0f, 1f), -size)
            if (p > 0.125f) {
                graphics2D.lineTo(size, -size + (size * 2 * ((p - 0.125f) / 0.25f)).coerceIn(0f, 2 * size))
            }
            if (p > 0.375f) {
                graphics2D.lineTo(size - (size * 2 * ((p - 0.375f) / 0.25f)).coerceIn(0f, 2 * size), size)
            }
            if (p > 0.625f) {
                graphics2D.lineTo(-size, size - (size * 2 * ((p - 0.625f) / 0.25f)).coerceIn(0f, 2 * size))
            }
            if (p > 0.875f) {
                graphics2D.lineTo(-size + (size * ((p - 0.875f) / 0.125f)).coerceIn(0f, size), -size)
            }
            graphics2D.strokePath()
            val blockPos = minecraft.gameMode?.destroyBlockPos ?: return
            val block = level.getBlockState(blockPos)
            val progressPerTick = player.getDestroySpeed(block) / (
                block.getDestroySpeed(
                    level,
                    blockPos,
                ) * (if (player.hasCorrectToolForDrops(block)) 30f else 100f)
                )
            if (progressPerTick <= 0f) return
            val remainingProgress = 1.0f - breakProgress
            val remainSecs = remainingProgress / progressPerTick / 20.0f
            val displaySec = String.format("%.1fs", remainSecs)
            graphics2D.fillStyle = color
            graphics2D.textStyle.size = size
            graphics2D.textStyle.font = "infinite_regular"
            graphics2D.textStyle.shadow = true
            graphics2D.textCentered(displaySec, 0, size * 1.5)
        }
        graphics2D.pop() // 全体の translate/scale を終了
    }
}
