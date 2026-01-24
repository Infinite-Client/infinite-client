package org.infinite.infinite.features.local.combat.throwable.projectile

import net.minecraft.world.item.*
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.throwable.ThrowableFeature
import org.infinite.libs.minecraft.projectile.AbstractProjectile

object ThrowableProjectile : AbstractProjectile() {
    private val feature: ThrowableFeature = InfiniteClient.localFeatures.combat.throwableFeature
    override var drag: Double = 0.99 // 初期値

    // AbstractProjectile側の実装に合わせて物理定数を管理
    override var gravity: Double = 0.03
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

            is FishingRodItem -> {
                gravity = 0.04
                drag = 0.92 // 釣り竿特有の強い空気抵抗
                1.5
            }

            is ThrowablePotionItem -> {
                gravity = 0.05
                drag = 0.99
                ThrowablePotionItem.PROJECTILE_SHOOT_POWER.toDouble()
            }

            is TridentItem -> {
                if (player.useItemRemainingTicks <= 0 && player.ticksUsingItem < 10) return null
                gravity = 0.05
                drag = 0.99
                2.5 // 安定して飛ぶデフォルト値
            }

            is ExperienceBottleItem -> {
                gravity = 0.07
                drag = 0.99
                0.7
            }

            else -> {
                drag = 0.99 // デフォルトに戻す
                // (前回の when 節の残りをここに配置)
                return null
            }
        }

        // 2. 基本情報のセットアップ
        val startPos = player.getEyePosition(0f)
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.selectedEntity

        return if (lockOnTarget != null) {
            // --- エンティティを狙う場合 ---

            // ポーションの場合は「目」ではなく「足元」を狙うようにターゲット位置を調整
            val targetAdjusted = if (itemStack.item is ThrowablePotionItem) {
                lockOnTarget.position() // 足元の座標
            } else {
                // 通常はエンティティの中心〜目を狙う（analysisAdvanced内部で処理）
                null
            }

            analysisAdvanced(
                basePower = velocity,
                target = lockOnTarget,
                startPos = startPos,
                iterations = 3,
                overrideTargetPos = targetAdjusted, // 必要に応じて足元を渡す
            ).let { result ->
                // Ping補正
                val latencyTicks = (minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0) / 50.0
                val latencyOffset = lockOnTarget.deltaMovement.scale(latencyTicks)
                result.copy(hitPos = result.hitPos.add(latencyOffset))
            }
        } else {
            // --- 地形を狙う場合 ---
            analysisStaticPos(velocity, getTargetPos(feature.maxReach.value.toDouble()) ?: return null, startPos)
        }
    }

    private fun isThrowableItem(stack: ItemStack): Boolean = stack.item is SnowballItem || stack.item is EggItem || stack.item is EnderpearlItem ||
        stack.item is ThrowablePotionItem || stack.item is ExperienceBottleItem || stack.item is TridentItem
}
