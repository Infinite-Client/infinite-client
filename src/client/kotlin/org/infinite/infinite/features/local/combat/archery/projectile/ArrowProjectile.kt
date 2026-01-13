package org.infinite.infinite.features.local.combat.archery.projectile

import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.libs.interfaces.MinecraftInterface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
    )

    /**
     * 現在の状況を分析し、最適な角度と経路の状態を返す
     */
    fun analyze(): TrajectoryAnalysis? {
        val player = this@ArrowProjectile.player ?: return null
        val item = player.mainHandItem

        // 基本の初速（弓の引き具合）
        val basePower = when (item.item) {
            Items.BOW -> BowItem.getPowerForTime(player.ticksUsingItem).toDouble() * 3.0
            Items.CROSSBOW -> 3.15
            else -> return null
        }
        if (basePower < 0.1) return null

        // ロックオン中の敵がいればそれを優先、いなければ視線の先
        val lockOnTarget = InfiniteClient.localFeatures.combat.lockOnFeature.lockedEntity as? LivingEntity
        var targetPos = lockOnTarget?.eyePosition ?: getTargetPos() ?: return null

        // 自分自身の移動ベクトル（慣性）
        val playerVel =
            Vec3(player.deltaMovement.x, if (player.onGround()) 0.0 else player.deltaMovement.y, player.deltaMovement.z)

        var finalXRot = 0.0
        var finalYRot = 0.0

        // --- 予測計算の反復 (Iterative Prediction) ---
        // ターゲットの移動と、矢の飛行時間を相互に計算して収束させる
        repeat(3) {
            val startPos = player.getArrowSpawnPosition()
            val dx = targetPos.x - startPos.x
            val dz = targetPos.z - startPos.z
            val horizontalDist = sqrt(dx * dx + dz * dz)

            // yRotの計算
            finalYRot = if (horizontalDist < 0.0001) player.yRot.toDouble() else (Mth.atan2(-dx, dz) * (180.0 / PI))

            // xRotの計算（自己ベクトルを考慮したシミュレーションが必要なため、solveAnglesを微調整）
            finalXRot = solveAnglesWithInertia(basePower, playerVel, targetPos, maxStep) ?: return null

            // 飛行時間（Ticks）を推定し、ターゲットの位置を更新
            if (lockOnTarget != null) {
                val travelTime = estimateTravelTime(basePower, finalXRot, targetPos)
                val targetVel = lockOnTarget.deltaMovement
                // 未来の位置 = 現在の位置 + (速度 * 時間)
                targetPos = lockOnTarget.eyePosition.add(targetVel.scale(travelTime))
            }
        }

// analyze() メソッド内での修正
        val status = verifyPathWithUncertainty(basePower, playerVel, finalXRot, finalYRot, targetPos)

        return TrajectoryAnalysis(finalXRot, finalYRot, status)
    }

    /**
     * プレイヤーの慣性を含めて角度を解く
     */
    private fun solveAnglesWithInertia(v: Double, pVel: Vec3, target: Vec3, steps: Int): Double? {
        val p = player ?: return null
        val startPos = p.getArrowSpawnPosition()
        val dy = target.y - startPos.y
        val horizontalDist = sqrt((target.x - startPos.x).pow(2) + (target.z - startPos.z).pow(2))

        var low = -89.9
        var high = 89.9

        repeat(precision) {
            val mid = (low + high) / 2.0
            // ここでのシミュレーションは水平・垂直のみの簡易版だが、慣性の影響(垂直方向)を考慮
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
        var vY = (v * sin(pitchRad)) + initialVelY // 自分のY速度が加算される
        repeat(steps) {
            posX += vX
            posY += vY
            vX *= 0.99
            vY = (vY * 0.99) - 0.05
            if (posX >= targetX) return posY
        }
        return posY
    }

    private fun estimateTravelTime(v: Double, xRot: Double, target: Vec3): Double {
        val startPos = player?.getArrowSpawnPosition() ?: return 0.0
        val horizontalDist = sqrt((target.x - startPos.x).pow(2) + (target.z - startPos.z).pow(2))
        val vX = v * cos(xRot * PI / -180.0)
        // 簡易的な到達時間計算 (空気抵抗を考慮した平均速度でも良いが、概算で十分)
        return horizontalDist / (vX + 0.001)
    }

    // 設定値の同期
    private val maxStep: Int get() = InfiniteClient.localFeatures.combat.archeryFeature.simulationMaxSteps.value
    private val precision: Int get() = InfiniteClient.localFeatures.combat.archeryFeature.simulationPrecision.value
    private val maxReach: Int get() = InfiniteClient.localFeatures.combat.archeryFeature.maxReach.value

    /**
     * 複数の弾道をシミュレートし、遮蔽物を確認する
     */
    private fun verifyPathWithUncertainty(
        v: Double,
        pVel: Vec3, // 追加
        xRot: Double,
        yRot: Double,
        target: Vec3,
    ): PathStatus {
        // 1. 理想的な（ブレなし）弾道の確認
        val idealHit = simulateWithCollision(v, pVel, xRot, yRot, target)
        if (idealHit == PathStatus.Obstructed) return PathStatus.Obstructed

        // 2. わずかなバラツキ（不確実性）の確認
        // 1.0のinaccuracyを想定し、周囲をシミュレート
        val offsets = listOf(Pair(0.5, 0.5), Pair(-0.5, 0.5), Pair(0.5, -0.5), Pair(-0.5, -0.5))
        for (offset in offsets) {
            if (simulateWithCollision(
                    v,
                    pVel,
                    xRot + offset.first,
                    yRot + offset.second,
                    target,
                ) == PathStatus.Obstructed
            ) {
                return PathStatus.Uncertain
            }
        }

        return PathStatus.Clear
    }

    /**
     * 指定された角度で発射した際の、実際のレベル上での衝突判定
     */
    private fun simulateWithCollision(
        v: Double,
        pVel: Vec3, // 追加
        xRot: Double,
        yRot: Double,
        target: Vec3,
    ): PathStatus {
        val p = player ?: return PathStatus.Obstructed
        val lv = level ?: return PathStatus.Obstructed

        var currentPos = p.getArrowSpawnPosition()

        // 回転角を正規化された視線ベクトルに変換
        val f = -Mth.sin(yRot * (PI / 180.0)) * Mth.cos(xRot * (PI / 180.0))
        val g = -Mth.sin(xRot * (PI / 180.0))
        val h = Mth.cos(yRot * (PI / 180.0)) * Mth.cos(xRot * (PI / 180.0))

        // 初速度 = (視線方向 * 弓のパワー) + プレイヤーの移動速度
        var velVec = Vec3(f.toDouble(), g.toDouble(), h.toDouble()).scale(v).add(pVel)

        repeat(maxStep) {
            val nextPos = currentPos.add(velVec)

            // ブロック衝突判定
            val result =
                lv.clip(ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p))

            if (result.type != HitResult.Type.MISS) {
                // ターゲット（予測位置）に十分近ければ命中とみなす
                // 1.5ブロック以内なら当たり判定の箱に入ると仮定
                return if (result.location.distanceToSqr(target) < 1.5.pow(2)) {
                    PathStatus.Clear
                } else {
                    PathStatus.Obstructed
                }
            }

            // ターゲットを通り過ぎたか確認
            if (currentPos.distanceToSqr(target) < 1.0) return PathStatus.Clear

            currentPos = nextPos
            // 矢の物理演算 (空気抵抗 0.99, 重力 0.05)
            velVec = velVec.scale(0.99).subtract(0.0, 0.05, 0.0)
        }
        return PathStatus.Clear
    }

    // --- 既存のヘルパーメソッド ---

    private fun getTargetPos(): Vec3? {
        val player = this@ArrowProjectile.player ?: return null
        val hitResult =
            ProjectileUtil.getHitResultOnViewVector(player, { !it.isSpectator && it.isPickable }, maxReach.toDouble())
        return if (hitResult.type != HitResult.Type.MISS) {
            hitResult.location
        } else {
            player.getEyePosition(1.0f).add(player.lookAngle.normalize().scale(maxReach.toDouble()))
        }
    }

    private fun LivingEntity.getArrowSpawnPosition(): Vec3 {
        return this.getEyePosition(0f).subtract(0.0, 0.1, 0.0)
    }
}
