package org.infinite.infinite.features.local.combat.throwable.projectile

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.item.ExperienceBottleItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.ThrowablePotionItem
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.throwable.ThrowableFeature
import org.infinite.libs.minecraft.projectile.AbstractProjectile
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

class ThrowableProjectile(private val feature: ThrowableFeature) : AbstractProjectile() {

    override var gravity: Double = 0.03
    override val drag: Double = 0.99
    override val precision: Int get() = feature.simulationPrecision.value
    override val maxStep: Int get() = feature.simulationMaxSteps.value

    fun analyze(): TrajectoryAnalysis? {
        val player = this.player ?: return null

        val hand = if (isThrowableItem(player.offhandItem)) {
            net.minecraft.world.InteractionHand.OFF_HAND
        } else {
            net.minecraft.world.InteractionHand.MAIN_HAND
        }
        val itemStack = player.getItemInHand(hand)

        // アイテムごとの物理定数設定
        val velocity = when (itemStack.item) {
            is EnderpearlItem, is EggItem, is SnowballItem -> {
                gravity = 0.03 // 雪玉、卵、パール
                1.5
            }

            is ExperienceBottleItem -> {
                gravity = 0.07 // エンチャントの瓶は重い
                0.7
            }

            is ThrowablePotionItem -> {
                gravity = 0.05 // スプラッシュポーション
                0.5
            }

            else -> return null
        }

        val startPos = player.getEyePosition(0f)
        val playerVel = player.deltaMovement
        val currentPitch = player.xRot.toDouble()
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.lockedEntity as? LivingEntity

        return if (lockOnTarget != null) {
            // --- エンティティロックオン時 ---
            analysisAdvanced(
                basePower = velocity,
                target = lockOnTarget,
                startPos = startPos,
                iterations = 3,
            ).let { analysis ->
                // Ping補正を最終予測位置に適用
                val latencyTicks = (minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0) / 50.0
                val latencyOffset = lockOnTarget.deltaMovement.scale(latencyTicks)
                analysis.copy(hitPos = analysis.hitPos.add(latencyOffset))
            }
        } else {
            // --- ターゲット座標指定時 (クロスヘア方向) ---
            val lookTarget = getTargetPos(feature.maxReach.value.toDouble()) ?: return null
            val dx = lookTarget.x - startPos.x
            val dz = lookTarget.z - startPos.z
            val horizontalDist = sqrt(dx * dx + dz * dz)
            val yRot = if (horizontalDist < 0.0001) player.yRot.toDouble() else (atan2(-dx, dz) * (180.0 / PI))

            // 最大射程の境界を特定
            val limitUpper = currentPitch - 90.0
            val maxRangePitch = findMaxRangeAngle(velocity, playerVel.y, limitUpper, currentPitch)
            val (maxDist, _) = simulateForDistance(velocity, maxRangePitch, playerVel.y, lookTarget.y - startPos.y)

            if (horizontalDist > maxDist) {
                // 射程外
                TrajectoryAnalysis(maxRangePitch, yRot, PathStatus.Obstructed, lookTarget, maxStep, false)
            } else {
                // 射程内: 高射角と低射角を計算
                val highPitch = solvePitchStrict(velocity, playerVel.y, lookTarget, startPos, limitUpper, maxRangePitch)
                val lowPitch =
                    solvePitchStrict(velocity, playerVel.y, lookTarget, startPos, maxRangePitch, currentPitch)

                // 地形チェック
                val highRes = verifyPath(velocity, highPitch, yRot, lookTarget, startPos)
                val lowRes = verifyPath(velocity, lowPitch, yRot, lookTarget, startPos)

                when {
                    lowRes.status == PathStatus.Clear ->
                        TrajectoryAnalysis(lowPitch, yRot, PathStatus.Clear, lookTarget, lowRes.ticks, false)

                    highRes.status == PathStatus.Clear ->
                        TrajectoryAnalysis(highPitch, yRot, PathStatus.Clear, lookTarget, highRes.ticks, true)

                    else ->
                        TrajectoryAnalysis(highPitch, yRot, PathStatus.Obstructed, lookTarget, highRes.ticks, true)
                }
            }
        }
    }

    private fun isThrowableItem(stack: ItemStack): Boolean =
        stack.item is SnowballItem || stack.item is EggItem || stack.item is EnderpearlItem ||
            stack.item is ThrowablePotionItem || stack.item is ExperienceBottleItem

    private fun getTargetPos(reach: Double): Vec3? {
        val p = player ?: return null
        val hitResult = ProjectileUtil.getHitResultOnViewVector(p, { !it.isSpectator && it.isPickable }, reach)
        return if (hitResult.type != HitResult.Type.MISS) {
            hitResult.location
        } else {
            p.getEyePosition(1.0f).add(p.lookAngle.normalize().scale(reach))
        }
    }
}
