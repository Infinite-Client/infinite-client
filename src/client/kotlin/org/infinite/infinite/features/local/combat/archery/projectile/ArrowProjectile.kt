package org.infinite.infinite.features.local.combat.archery.projectile

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.Items
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.archery.ArcheryFeature
import org.infinite.libs.minecraft.projectile.AbstractProjectile
import kotlin.math.atan2
import kotlin.math.sqrt

class ArrowProjectile(private val feature: ArcheryFeature) : AbstractProjectile() {

    override val gravity: Double = 0.04
    override val drag: Double = 0.99
    override val precision: Int get() = feature.simulationPrecision.value
    override val maxStep: Int get() = feature.simulationMaxSteps.value

    fun analyze(): TrajectoryAnalysis? {
        val player = this.player ?: return null
        val item = player.mainHandItem

        // 1. 発射パワーの決定
        val basePower = when (item.item) {
            Items.BOW -> {
                if (player.useItemRemainingTicks <= 0 && player.ticksUsingItem == 0) return null
                BowItem.getPowerForTime(player.ticksUsingItem).toDouble() * 3.0
            }

            Items.CROSSBOW -> {
                if (!CrossbowItem.isCharged(item)) return null
                3.15
            }

            else -> return null
        }
        if (basePower < 0.1) return null

        // 2. 基本情報のセットアップ
        val startPos = player.getEyePosition(0f).subtract(0.0, 0.1, 0.0)
        // プレイヤーの移動速度（慣性）。地上にいるときは水平方向のみ考慮されることが多い
        val playerVel = player.deltaMovement

        // 3. ターゲットの特定と計算
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.lockedEntity as? LivingEntity

        return if (lockOnTarget != null) {
            // ロックオン中の場合：高度な反復予測（偏差撃ち + 山なり回避）を実行
            // 基底クラスの analysisAdvanced を呼び出す
            analysisAdvanced(
                basePower = basePower,
                playerVel = playerVel,
                target = lockOnTarget,
                startPos = startPos,
                iterations = 3, // 3回反復して精度を確保
            ).let { result ->
                // Ping補正を結果の hitPos に反映（UI表示用）
                val latencyTicks = (minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0) / 50.0
                val latencyOffset = lockOnTarget.deltaMovement.scale(latencyTicks)
                result.copy(hitPos = result.hitPos.add(latencyOffset))
            }
        } else {
            // ロックオンしていない場合：レティクルの先を狙う（従来の計算）
            val targetPos = getTargetPos() ?: return null
            val dx = targetPos.x - startPos.x
            val dz = targetPos.z - startPos.z
            val horizontalDist = sqrt(dx * dx + dz * dz)
            val yRot =
                if (horizontalDist < 0.0001) player.yRot.toDouble() else (atan2(-dx, dz) * (180.0 / Math.PI))

            // solveOptimalAngle を使って、遮蔽物がある場合に山なり弾道を選択
            solveOptimalAngle(basePower, playerVel, targetPos, startPos, yRot)
        }
    }

    private fun getTargetPos(): Vec3? {
        val p = player ?: return null
        val reach = feature.maxReach.value.toDouble()
        val hitResult = ProjectileUtil.getHitResultOnViewVector(p, { !it.isSpectator && it.isPickable }, reach)

        return if (hitResult.type != HitResult.Type.MISS) {
            hitResult.location
        } else {
            p.getEyePosition(1.0f).add(p.lookAngle.normalize().scale(reach))
        }
    }
}
