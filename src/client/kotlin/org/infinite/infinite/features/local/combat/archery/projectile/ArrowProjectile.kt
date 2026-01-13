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
        val p = player ?: return null
        val target = getTargetPos() ?: return null
        val item = p.mainHandItem ?: return null

        val velocity = when (item.item) {
            Items.BOW -> BowItem.getPowerForTime(p.ticksUsingItem).toDouble() * 3.0
            Items.CROSSBOW -> 3.15
            else -> return null
        }
        if (velocity < 0.1) return null

        val startPos = p.getArrowSpawnPosition()
        val dx = target.x - startPos.x
        val dz = target.z - startPos.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        // 1. yRotの計算: 水平移動がない場合は現在の向きを維持
        val yRot = if (horizontalDist < 0.0001) {
            p.yRot.toDouble()
        } else {
            (Mth.atan2(-dx, dz) * (180.0 / PI))
        }

        // 2. xRotの計算
        val xRot = solveAngles(velocity, target, maxStep) ?: return null

        // 3. 経路の検証
        val status = verifyPathWithUncertainty(velocity, xRot, yRot, target)

        return TrajectoryAnalysis(xRot, yRot, status)
    }

    private fun solveAngles(v: Double, target: Vec3, steps: Int): Double? {
        val p = player ?: return null
        val startPos = p.getArrowSpawnPosition()
        val dx = target.x - startPos.x
        val dy = target.y - startPos.y
        val dz = target.z - startPos.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        // 真上・真下付近の特殊処理
        if (horizontalDist < 0.001) {
            return if (dy > 0) -90.0 else 90.0
        }

        // 二分探索の範囲: -90(上) ～ 90(下)
        // simulateTrajectory 内での pitchDeg は数学的な角度（上がプラス）として扱うため
        // 一旦数学的な座標系で計算する
        var low = -89.9
        var high = 89.9

        // ターゲットが届く範囲にあるかどうかの簡易チェック
        // (最大仰角 45度付近でシミュレートして dy に届かないなら low/high を調整)

        repeat(precision) {
            val mid = (low + high) / 2.0
            val hitY = simulateTrajectory(v, mid, horizontalDist, steps)
            if (hitY < dy) low = mid else high = mid
        }

        // 数学的な角度（上がプラス）を Minecraft の Pitch（上がマイナス）に変換
        return -low
    }

    // 設定値の同期
    private val maxStep: Int get() = InfiniteClient.localFeatures.combat.archeryFeature.simulationMaxSteps.value
    private val precision: Int get() = InfiniteClient.localFeatures.combat.archeryFeature.simulationPrecision.value
    private val maxReach: Int get() = InfiniteClient.localFeatures.combat.archeryFeature.maxReach.value

    /**
     * 複数の弾道をシミュレートし、遮蔽物を確認する
     */
    private fun verifyPathWithUncertainty(v: Double, xRot: Double, yRot: Double, target: Vec3): PathStatus {
        // 1. 理想的な（ブレなし）弾道の確認
        val idealHit = simulateWithCollision(v, xRot, yRot, target)
        if (idealHit == PathStatus.Obstructed) return PathStatus.Obstructed

        // 2. わずかなバラツキ（不確実性）の確認
        // 1.0のinaccuracyを想定し、上下左右にわずかにずらしてシミュレート
        val offsets = listOf(Pair(0.5, 0.5), Pair(-0.5, 0.5), Pair(0.5, -0.5), Pair(-0.5, -0.5))
        for (offset in offsets) {
            if (simulateWithCollision(v, xRot + offset.first, yRot + offset.second, target) == PathStatus.Obstructed) {
                return PathStatus.Uncertain
            }
        }

        return PathStatus.Clear
    }

    /**
     * 指定された角度で発射した際の、実際のレベル上での衝突判定
     */
    private fun simulateWithCollision(v: Double, xRot: Double, yRot: Double, target: Vec3): PathStatus {
        val p = player ?: return PathStatus.Obstructed
        val lv = level ?: return PathStatus.Obstructed

        var currentPos = p.getArrowSpawnPosition()

        // 回転角をベクトルに変換
        val f = -Mth.sin(yRot * (PI / 180.0)) * Mth.cos(xRot * (PI / 180.0))
        val g = -Mth.sin(xRot * (PI / 180.0))
        val h = Mth.cos(yRot * (PI / 180.0)) * Mth.cos(xRot * (PI / 180.0))
        var velVec = Vec3(f.toDouble(), g.toDouble(), h.toDouble()).normalize().scale(v)

        repeat(maxStep) {
            val nextPos = currentPos.add(velVec)

            // ブロック衝突判定
            val result =
                lv.clip(ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p))

            if (result.type != HitResult.Type.MISS) {
                // ターゲットに十分近い（1.5ブロック以内）場所での衝突なら命中とみなす
                return if (result.location.distanceToSqr(target) < 1.5.pow(2)) {
                    PathStatus.Clear
                } else {
                    PathStatus.Obstructed
                }
            }

            // ターゲットを通り過ぎたか（水平距離ベース）
            val distToTarget = currentPos.distanceToSqr(target)
            if (distToTarget < 1.0) return PathStatus.Clear

            currentPos = nextPos
            velVec = velVec.scale(0.99).subtract(0.0, 0.05, 0.0)
        }
        return PathStatus.Clear
    }

    // --- 既存のヘルパーメソッド ---

    private fun getTargetPos(): Vec3? {
        val player = this@ArrowProjectile.player ?: return null
        val hitResult =
            ProjectileUtil.getHitResultOnViewVector(player, { !it.isSpectator && it.isPickable }, maxReach.toDouble())
        return if (hitResult != null && hitResult.type != HitResult.Type.MISS) {
            hitResult.location
        } else {
            player.getEyePosition(1.0f).add(player.lookAngle.normalize().scale(maxReach.toDouble()))
        }
    }

    private fun LivingEntity.getArrowSpawnPosition(): Vec3 {
        return this.getEyePosition(0f).subtract(0.0, 0.1, 0.0)
    }

    private fun simulateTrajectory(v: Double, pitchDeg: Double, targetX: Double, steps: Int): Double {
        val pitchRad = pitchDeg * PI / 180.0
        var posX = 0.0
        var posY = 0.0
        var vX = v * cos(pitchRad)
        var vY = v * sin(pitchRad)
        repeat(steps) {
            posX += vX
            posY += vY
            vX *= 0.99
            vY = (vY * 0.99) - 0.05
            if (posX >= targetX) return posY
        }
        return posY
    }
}
