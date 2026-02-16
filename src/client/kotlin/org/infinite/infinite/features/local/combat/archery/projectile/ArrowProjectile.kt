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
                // 実際に弓を引いている時間を正確に取得
                val useTicks = player.ticksUsingItem
                if (useTicks <= 0) return null
                // 弓のパワー(0.0~1.0) * 標準最大速度(3.0)
                BowItem.getPowerForTime(useTicks).toDouble() * 3.0
            }

            Items.CROSSBOW -> {
                if (!CrossbowItem.isCharged(item)) return null
                3.15 // クロスボウの標準初速
            }

            else -> return null
        }

        // 引き絞り不足（パワーが低い）なら描画しない
        if (basePower < 0.5) return null

        // 2. 開始位置の修正
        // getEyePosition(1.0f) を使用して現在のフレームの正確な位置を取得
        val startPos = player.getEyePosition(1.0f)

        // 3. ターゲットの特定と解析
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.selectedEntity

        val analysis = if (lockOnTarget != null) {
            // 足元(position)ではなく、中心(boundingBox.center)を狙う
            val targetCenter = lockOnTarget.boundingBox.center

            analysisAdvanced(
                basePower = basePower,
                target = lockOnTarget,
                startPos = startPos,
                iterations = 5,
                overrideTargetPos = targetCenter, // ここで中心を指定
            )
        } else {
            val reach = feature.maxReach.value.toDouble()
            val lookTarget = getTargetPos(reach) ?: return null
            analysisStaticPos(basePower, lookTarget, startPos)
        }

        // 4. ネットワーク遅延の補正と結果の調整
        return analysis.let {
            // Rustから返る Pitch は物理座標系（上が正）である可能性があるため、
            // もしエイムが上下逆になる場合はここで xRot を反転させる調整が必要
            it.copy(
                // 念のため hitPos の計算にPing補正（必要な場合のみ）
                hitPos = it.hitPos,
            )
        }
    }
}
