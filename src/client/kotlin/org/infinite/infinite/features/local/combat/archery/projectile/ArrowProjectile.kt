package org.infinite.infinite.features.local.combat.archery.projectile

import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.Items
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.archery.ArcheryFeature
import org.infinite.libs.minecraft.projectile.AbstractProjectile

object ArrowProjectile : AbstractProjectile() {
    private val feature: ArcheryFeature = InfiniteClient.localFeatures.combat.archeryFeature
    override val gravity: Double = 0.05
    override var drag: Double = 0.99
    override val precision: Int get() = feature.simulationPrecision.value
    override val maxStep: Int get() = feature.simulationMaxSteps.value

    fun analyze(): TrajectoryAnalysis? {
        val player = this.player ?: return null
        val item = player.mainHandItem

        // 1. 発射パワー（初速）の決定
        val basePower = when (item.item) {
            Items.BOW -> {
                // 溜め時間に応じたパワー計算 (最大3.0)
                if (player.useItemRemainingTicks <= 0 && player.ticksUsingItem == 0) return null
                BowItem.getPowerForTime(player.ticksUsingItem).toDouble() * 3.0
            }

            Items.CROSSBOW -> {
                // クロスボウは溜めに関わらず一定 (3.15)
                if (!CrossbowItem.isCharged(item)) return null
                3.15
            }

            else -> return null
        }

        // 最低限のパワーがない場合は計算しない
        if (basePower < 0.1) return null

        // 2. 基本情報のセットアップ
        // 矢の発射位は視線よりわずかに下
        val startPos = player.getEyePosition(0f).subtract(0.0, 0.1, 0.0)
        player.deltaMovement

        // 3. ターゲットの特定と解析
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.selectedEntity

        return if (lockOnTarget != null) {
            // --- エンティティを狙う場合 ---
            // 偏差予測を行い、高射角/低射角を自動選択。ターゲットの上下シフト（オフセット）あり。
            analysisAdvanced(
                basePower = basePower,
                target = lockOnTarget,
                startPos = startPos,
                iterations = 3,
            ).let { result ->
                // UI表示用にPing（通信遅延）を考慮した位置補正を加える
                val latencyTicks = (minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0) / 50.0
                val latencyOffset = lockOnTarget.deltaMovement.scale(latencyTicks)
                result.copy(hitPos = result.hitPos.add(latencyOffset))
            }
        } else {
            val reach = feature.maxReach.value.toDouble()
            val lookTarget = getTargetPos(reach) ?: return null

            analysisStaticPos(
                basePower = basePower,
                targetPos = lookTarget,
                startPos = startPos,
            )
        }
    }
}
