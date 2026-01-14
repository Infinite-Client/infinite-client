package org.infinite.infinite.features.local.combat.throwable.projectile

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.item.ExperienceBottleItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.ThrowablePotionItem
import net.minecraft.world.item.TridentItem
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.throwable.ThrowableFeature
import org.infinite.libs.minecraft.projectile.AbstractProjectile

object ThrowableProjectile : AbstractProjectile() {
    private val feature: ThrowableFeature = InfiniteClient.localFeatures.combat.throwableFeature

    // AbstractProjectile側の実装に合わせて物理定数を管理
    override var gravity: Double = 0.03
    override val drag: Double = 0.99
    override val precision: Int get() = feature.simulationPrecision.value
    override val maxStep: Int get() = feature.simulationMaxSteps.value

    fun analyze(): TrajectoryAnalysis? {
        val player = this.player ?: return null

        // 1. 使用アイテムの特定と物理定数の動的設定
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
            is TridentItem -> {
                if (player.useItemRemainingTicks <= 0 && player.ticksUsingItem < 10) return null
                gravity = 0.05
                // 3.0 相当。TridentItem.PROJECTILE_SHOOT_POWER は 1.0f なので、
                // トライデント特有の倍率が必要な場合は直接数値を指定するか調整してください。
                2.5 // 安定して飛ぶデフォルト値
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

        // 2. 基本情報のセットアップ
        val startPos = player.getEyePosition(0f)
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.lockedEntity as? LivingEntity

        return if (lockOnTarget != null) {
            // --- エンティティを狙う場合 ---
            analysisAdvanced(
                basePower = velocity,
                target = lockOnTarget,
                startPos = startPos,
                iterations = 3,
            ).let { result ->
                // Ping補正適用
                val latencyTicks = (minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0) / 50.0
                val latencyOffset = lockOnTarget.deltaMovement.scale(latencyTicks)
                result.copy(hitPos = result.hitPos.add(latencyOffset))
            }
        } else {
            // --- 地形を狙う場合（ここを ArrowProjectile と同様の処理に修正） ---
            val lookTarget = getTargetPos(feature.maxReach.value.toDouble()) ?: return null

            // AbstractProjectile に実装した最適化済みの地形解析を使用
            analysisStaticPos(
                basePower = velocity,
                targetPos = lookTarget,
                startPos = startPos,
            )
        }
    }

    private fun isThrowableItem(stack: ItemStack): Boolean =
        stack.item is SnowballItem || stack.item is EggItem || stack.item is EnderpearlItem ||
            stack.item is ThrowablePotionItem || stack.item is ExperienceBottleItem || stack.item is TridentItem

    private fun getTargetPos(reach: Double): Vec3? {
        val p = player ?: return null
        // エンティティを含めたレイキャスト。MISSなら射程限界の空中をターゲットにする
        val hitResult = ProjectileUtil.getHitResultOnViewVector(p, { !it.isSpectator && it.isPickable }, reach)
        return if (hitResult.type != HitResult.Type.MISS) {
            hitResult.location
        } else {
            p.getEyePosition(1.0f).add(p.lookAngle.normalize().scale(reach))
        }
    }
}
