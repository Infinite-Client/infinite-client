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

        val velocity = when (itemStack.item) {
            is EnderpearlItem, is EggItem, is SnowballItem -> {
                gravity = 0.03
                1.5
            }
            is ExperienceBottleItem -> {
                gravity = 0.07
                0.7
            }
            is ThrowablePotionItem -> {
                gravity = 0.05
                0.5
            }
            else -> return null
        }

        val startPos = player.getEyePosition(0f)
        val playerVel = player.deltaMovement
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.lockedEntity as? LivingEntity

        return if (lockOnTarget != null) {
            // 【修正点】analysisAdvanced に heightBias を渡す
            val analysis = analysisAdvanced(
                basePower = velocity,
                playerVel = playerVel,
                target = lockOnTarget,
                startPos = startPos,
                iterations = 3,
            )

            // Ping補正を最終予測位置（hitPos）にのみ適用
            val latencyTicks = (minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0) / 50.0
            val latencyOffset = lockOnTarget.deltaMovement.scale(latencyTicks)

            analysis.copy(hitPos = analysis.hitPos.add(latencyOffset))
        } else {
            // --- ターゲットがいない場合: クロスヘア方向への射角計算 ---
            val lookTarget = getTargetPos(feature.maxReach.value.toDouble()) ?: return null
            val dx = lookTarget.x - startPos.x
            val dz = lookTarget.z - startPos.z
            val horizontalDist = sqrt(dx * dx + dz * dz)
            val yRot = if (horizontalDist < 0.0001) player.yRot.toDouble() else (atan2(-dx, dz) * (180.0 / PI))

            solveOptimalAngle(velocity, playerVel, lookTarget, startPos, yRot)
        }
    }

    private fun isThrowableItem(stack: ItemStack): Boolean = stack.item is SnowballItem || stack.item is EggItem || stack.item is EnderpearlItem || stack.item is ThrowablePotionItem || stack.item is ExperienceBottleItem

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
