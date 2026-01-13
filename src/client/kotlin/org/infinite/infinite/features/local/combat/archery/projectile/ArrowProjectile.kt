package org.infinite.infinite.features.local.combat.archery.projectile

import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.archery.ArcheryFeature
import org.infinite.libs.interfaces.MinecraftInterface
import kotlin.math.*

class ArrowProjectile : MinecraftInterface() {

    enum class PathStatus {
        Clear, // 遮蔽物なし
        Obstructed, // 確実にブロックに衝突する
        Uncertain, // 精度（バラツキ）によっては衝突する可能性がある
    }

    data class TrajectoryAnalysis(
        val xRot: Double,
        val yRot: Double,
        val status: PathStatus,
        val hitPos: Vec3, // 衝突した実際の座標を追加
    )

    /**
     * 現在の状況を分析し、最適な角度と経路の状態を返す
     */
    fun analyze(): TrajectoryAnalysis? {
        val player = this@ArrowProjectile.player ?: return null
        val item = player.mainHandItem
        // --- 修正点: クロスボウのパワー取得ロジック ---
        val basePower = when (item.item) {
            Items.BOW -> {
                if (player.useItemRemainingTicks <= 0 && player.ticksUsingItem == 0) return null
                BowItem.getPowerForTime(player.ticksUsingItem).toDouble() * 3.0
            }

            Items.CROSSBOW -> {
                if (!CrossbowItem.isCharged(item)) return null
                3.15 // クロスボウの矢の初速は 3.15 固定
            }

            else -> return null
        }

        if (basePower < 0.1) return null
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.lockedEntity as? LivingEntity
        var targetPos = lockOnTarget?.eyePosition ?: getTargetPos() ?: return null

        // 1. Ping補正: クライアントが見ている位置をサーバー上の現在位置へ修正 [cite: 2025-12-16]
        val latency = (minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0) / 1000.0
        if (lockOnTarget != null) {
            // 20 ticks = 1秒。latency分の移動量を加算 [cite: 2025-12-16]
            targetPos = targetPos.add(lockOnTarget.deltaMovement.scale(latency * 20.0))
        }

        val playerVel =
            Vec3(player.deltaMovement.x, if (player.onGround()) 0.0 else player.deltaMovement.y, player.deltaMovement.z)
        var finalXRot = 0.0
        var finalYRot = 0.0

        // 2. 予測計算の反復 (Iterative Prediction)
        // 飛行時間とターゲットの移動を相互に計算して収束させる [cite: 2025-12-16]
        repeat(3) {
            val startPos = player.getArrowSpawnPosition()
            val dx = targetPos.x - startPos.x
            val dz = targetPos.z - startPos.z
            val horizontalDist = sqrt(dx * dx + dz * dz)

            finalYRot = if (horizontalDist < 0.0001) player.yRot.toDouble() else (Mth.atan2(-dx, dz) * (180.0 / PI))
            finalXRot = solveAnglesWithInertia(basePower, playerVel, targetPos, maxStep) ?: return null

            if (lockOnTarget != null) {
                val travelTime = estimateTravelTime(basePower, finalXRot, targetPos)

                // 3. 加速予測: 敵の現在の速度で未来の位置を計算 [cite: 2025-12-16]
                val targetVel = lockOnTarget.deltaMovement
                targetPos = lockOnTarget.eyePosition.add(targetVel.scale(travelTime + (latency * 20.0)))
            }
        }

        // 4. 遮蔽物と水中の確認
        val status = verifyPathWithUncertainty(basePower, playerVel, finalXRot, finalYRot, targetPos)

        return TrajectoryAnalysis(finalXRot, finalYRot, status, targetPos)
    }

    private fun solveAnglesWithInertia(v: Double, pVel: Vec3, target: Vec3, steps: Int): Double? {
        val p = player ?: return null
        val startPos = p.getArrowSpawnPosition()
        val dy = target.y - startPos.y
        val horizontalDist = sqrt((target.x - startPos.x).pow(2) + (target.z - startPos.z).pow(2))

        var low = -89.9
        var high = 89.9

        repeat(precision) {
            val mid = (low + high) / 2.0
            val hitY = simulateTrajectoryWithInertia(v, mid, pVel.y, horizontalDist, steps)
            if (hitY < dy) low = mid else high = mid
        }
        return -low
    }

    private fun simulateTrajectoryWithInertia(
        v: Double,
        pitchDeg: Double,
        initialVelY: Double,
        targetX: Double,
        steps: Int,
    ): Double {
        val pitchRad = pitchDeg * PI / 180.0
        var posX = 0.0
        var posY = 0.0
        var vX = v * cos(pitchRad)
        var vY = (v * sin(pitchRad)) + initialVelY

        repeat(steps) {
            posX += vX
            posY += vY
            vX *= 0.99
            vY = (vY * 0.99) - 0.05
            if (posX >= targetX) return posY
        }
        return posY
    }

    private fun verifyPathWithUncertainty(v: Double, pVel: Vec3, xRot: Double, yRot: Double, target: Vec3): PathStatus {
        val idealHit = simulateWithCollision(v, pVel, xRot, yRot, target)
        if (idealHit == PathStatus.Obstructed) return PathStatus.Obstructed

        // バラツキを考慮した4方向のオフセット確認 [cite: 2025-12-16]
        val offsets = listOf(Pair(0.5, 0.5), Pair(-0.5, 0.5), Pair(0.5, -0.5), Pair(-0.5, -0.5))
        for (offset in offsets) {
            if (simulateWithCollision(
                    v, pVel, xRot + offset.first, yRot + offset.second, target,
                ) == PathStatus.Obstructed
            ) {
                return PathStatus.Uncertain
            }
        }
        return PathStatus.Clear
    }

    private fun simulateWithCollision(v: Double, pVel: Vec3, xRot: Double, yRot: Double, target: Vec3): PathStatus {
        val p = player ?: return PathStatus.Obstructed
        val lv = level ?: return PathStatus.Obstructed
        var currentPos = p.getArrowSpawnPosition()

        val f = -Mth.sin(yRot * (PI / 180.0)) * Mth.cos(xRot * (PI / 180.0))
        val g = -Mth.sin(xRot * (PI / 180.0))
        val h = Mth.cos(yRot * (PI / 180.0)) * Mth.cos(xRot * (PI / 180.0))
        var velVec = Vec3(f.toDouble(), g.toDouble(), h.toDouble()).scale(v).add(pVel)

        // 設定された maxStep を使用してシミュレート [cite: 2026-01-12]
        repeat(maxStep) {
            val nextPos = currentPos.add(velVec)
            val result =
                lv.clip(ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p))

            if (result.type != HitResult.Type.MISS) {
                return if (result.location.distanceToSqr(target) < 2.25) PathStatus.Clear else PathStatus.Obstructed
            }

            // 5. 水中の物理演算: 流体状態を確認して減速比率を変更 [cite: 2025-12-16]
            val isInWater = lv.getFluidState(BlockPos.containing(nextPos)).isSource
            val drag = if (isInWater) 0.6 else 0.99 // 水中では 0.6 に失速 [cite: 2025-12-16]

            currentPos = nextPos
            velVec = velVec.scale(drag).subtract(0.0, 0.05, 0.0)

            if (currentPos.distanceToSqr(target) < 1.0) return PathStatus.Clear
        }
        return PathStatus.Clear
    }

    private fun estimateTravelTime(v: Double, xRot: Double, target: Vec3): Double {
        val startPos = player?.getArrowSpawnPosition() ?: return 0.0
        val horizontalDist = sqrt((target.x - startPos.x).pow(2) + (target.z - startPos.z).pow(2))
        val vX = v * cos(xRot * PI / -180.0)
        return horizontalDist / (vX + 0.001)
    }

    private fun getTargetPos(): Vec3? {
        val p = player ?: return null
        val hitResult =
            ProjectileUtil.getHitResultOnViewVector(p, { !it.isSpectator && it.isPickable }, maxReach.toDouble())
        return if (hitResult.type != HitResult.Type.MISS) {
            hitResult.location
        } else {
            p.getEyePosition(1.0f).add(p.lookAngle.normalize().scale(maxReach.toDouble()))
        }
    }

    private fun LivingEntity.getArrowSpawnPosition(): Vec3 = this.getEyePosition(0f).subtract(0.0, 0.1, 0.0)
    private val archeryFeature: ArcheryFeature get() = InfiniteClient.localFeatures.combat.archeryFeature
    private val maxStep: Int get() = archeryFeature.simulationMaxSteps.value
    private val precision: Int get() = archeryFeature.simulationPrecision.value
    private val maxReach: Int get() = archeryFeature.maxReach.value
}
