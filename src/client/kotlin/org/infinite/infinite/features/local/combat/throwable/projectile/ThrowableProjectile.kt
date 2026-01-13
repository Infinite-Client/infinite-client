package org.infinite.infinite.features.local.combat.throwable.projectile

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.Items
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.throwable.ThrowableFeature
import org.infinite.libs.minecraft.projectile.AbstractProjectile
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

class ThrowableProjectile(private val feature: ThrowableFeature) : AbstractProjectile() {

    override val gravity: Double = 0.03 // Throwable（投擲物）用の標準重力
    override val drag: Double = 0.99
    override val precision: Int get() = feature.simulationPrecision.value
    override val maxStep: Int get() = feature.simulationMaxSteps.value

    fun analyze(): TrajectoryAnalysis? {
        val player = this.player ?: return null
        val item = player.mainHandItem

        // 1. アイテムごとの初速設定
        val velocity = when (item.item) {
            Items.ENDER_PEARL, Items.EGG, Items.SNOWBALL -> 1.5
            Items.EXPERIENCE_BOTTLE -> 0.7
            Items.SPLASH_POTION, Items.LINGERING_POTION -> 0.5
            else -> return null
        }

        // 2. 基本情報のセットアップ
        val startPos = player.getEyePosition(0f)
        val playerVel = player.deltaMovement

        // 3. ターゲットの特定と計算（ロックオン優先）
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.lockedEntity as? LivingEntity

        return if (lockOnTarget != null) {
            // ロックオン中の場合：偏差予測と遮蔽物回避を行う
            analysisAdvanced(
                basePower = velocity,
                playerVel = playerVel,
                target = lockOnTarget,
                startPos = startPos,
                iterations = 3,
            ).let { result ->
                // Ping補正を結果に反映
                val latencyTicks = (minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0) / 50.0
                val latencyOffset = lockOnTarget.deltaMovement.scale(latencyTicks)
                result.copy(hitPos = result.hitPos.add(latencyOffset))
            }
        } else {
            // ロックオンなし：クロスヘアの先を狙う（低射角・高射角の最適化あり）
            val targetPos = getTargetPos(feature.maxReach.value.toDouble()) ?: return null

            val dx = targetPos.x - startPos.x
            val dz = targetPos.z - startPos.z
            val horizontalDist = sqrt(dx * dx + dz * dz)
            val yRot = if (horizontalDist < 0.0001) player.yRot.toDouble() else (atan2(-dx, dz) * (180.0 / PI))

            // 障害物がある場合は自動で山なり（高射角）を選択
            solveOptimalAngle(velocity, playerVel, targetPos, startPos, yRot)
        }
    }

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
