package org.infinite.libs.minecraft.projectile

import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.log.LogSystem
import org.infinite.utils.alpha
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

abstract class AbstractProjectile : MinecraftInterface() {
    enum class PathStatus { Clear, Obstructed, Uncertain }

    data class TrajectoryAnalysis(
        val xRot: Double,
        val yRot: Double,
        val status: PathStatus,
        val hitPos: Vec3,
        val travelTicks: Int, // 着弾までの予測時間
    )

    abstract val gravity: Double
    abstract val drag: Double
    abstract val precision: Int
    abstract val maxStep: Int

    fun analysisAdvanced(
        basePower: Double,
        playerVel: Vec3,
        target: Entity,
        startPos: Vec3,
        iterations: Int = 3,
    ): TrajectoryAnalysis {
        // --- 修正箇所: 狙点（Height Offset）の動的決定 ---

        // 1. ターゲットのベース座標（足元）を取得
        val targetBasePos = target.getPosition(minecraft.deltaTracker.gameTimeDeltaTicks)
        val eyeHeight = target.eyeHeight.toDouble()
        val distToTarget = targetBasePos.distanceTo(startPos)

        // 2. 狙う高さの係数 (0.0 = 足元, 1.0 = 頭部) を計算
        // 距離が遠いほど (例えば 30m以上)、または射角が急になるほど 0.0 に近づける
        // ここでは暫定的に「距離による減衰」を導入
        val distanceFactor = (1.0 - (distToTarget / 60.0)).coerceIn(0.2, 1.0)

        // 暫定的な狙点。最初は eyeHeight * distanceFactor で計算
        val currentTargetPos = targetBasePos.add(0.0, eyeHeight * distanceFactor, 0.0)

        var predictedPos = currentTargetPos
        var lastAnalysis = TrajectoryAnalysis(0.0, 0.0, PathStatus.Uncertain, predictedPos, 0)

        repeat(iterations) {
            val dx = predictedPos.x - startPos.x
            val dz = predictedPos.z - startPos.z
            val horizontalDist = sqrt(dx * dx + dz * dz)
            val yRot = if (horizontalDist < 0.0001) player?.yRot?.toDouble() ?: 0.0 else (atan2(-dx, dz) * (180.0 / PI))

            // 弾道計算
            val result = solveOptimalAngle(basePower, playerVel, predictedPos, startPos, yRot)
            lastAnalysis = result

            // --- 修正箇所: 射角による狙点の更なる調整 ---
            // 射角 (Pitch) が急（下向きが強い、あるいは山なりの頂点から落ちてくる）な場合、
            // さらに狙点を下げる (射角が 20度を超えたら徐々に足元へ)
            val pitchInfluence = (1.0 - (abs(result.xRot) / 45.0)).coerceIn(0.0, 1.0)
            val finalHeightOffset = eyeHeight * (distanceFactor * pitchInfluence).coerceIn(0.1, 1.0)

            // 予測位置の更新
            val targetVel = target.deltaMovement
            val predictedVelY = if (target.onGround()) 0.0 else targetVel.y

            // 基本の足元位置 ＋ 偏差 ＋ 動的に決めた高さ
            predictedPos = targetBasePos.add(
                targetVel.x * result.travelTicks,
                (predictedVelY * result.travelTicks) + finalHeightOffset,
                targetVel.z * result.travelTicks,
            )
        }

        if (predictedPos.y == 0.0) LogSystem.log("SAME")
        return lastAnalysis.copy(hitPos = predictedPos)
    }

    protected fun solveOptimalAngle(
        v: Double,
        pVel: Vec3,
        target: Vec3,
        start: Vec3,
        yRot: Double,
    ): TrajectoryAnalysis {
        // 1. 低射角 (-45度 〜 30度付近) を計算
        val lowPitch = solvePitch(v, pVel, target, start, -45.0, 30.0)
        val lowStatus = verifyPath(v, pVel, lowPitch, yRot, target, start)

        // 2. 高射角 (30度 〜 89度) を計算
        val highPitch = solvePitch(v, pVel, target, start, 30.0, 89.0)
        val highStatus = verifyPath(v, pVel, highPitch, yRot, target, start)

        // --- 選択ロジック ---

        // 両方通るなら、着弾が早い方（基本的に低射角）を選択
        if (lowStatus.status == PathStatus.Clear && highStatus.status == PathStatus.Clear) {
            return if (lowStatus.ticks <= highStatus.ticks) {
                TrajectoryAnalysis(lowPitch, yRot, PathStatus.Clear, target, lowStatus.ticks)
            } else {
                TrajectoryAnalysis(highPitch, yRot, PathStatus.Clear, target, highStatus.ticks)
            }
        }

        // 低射角だけが通るなら低射角
        if (lowStatus.status == PathStatus.Clear) {
            return TrajectoryAnalysis(lowPitch, yRot, PathStatus.Clear, target, lowStatus.ticks)
        }

        // 低射角がダメで、高射角が通るなら高射角（これが本来の山なり回避）
        if (highStatus.status == PathStatus.Clear) {
            return TrajectoryAnalysis(highPitch, yRot, PathStatus.Clear, target, highStatus.ticks)
        }

        // どちらもダメなら、マシな方（低射角）を返しておく
        return TrajectoryAnalysis(lowPitch, yRot, PathStatus.Obstructed, target, lowStatus.ticks)
    }

    private fun solvePitch(
        v: Double,
        pVel: Vec3,
        target: Vec3,
        start: Vec3,
        minAngle: Double,
        maxAngle: Double,
    ): Double {
        val dy = target.y - start.y
        val horizontalDist = sqrt((target.x - start.x).pow(2) + (target.z - start.z).pow(2))
        var low = minAngle
        var high = maxAngle

        repeat(precision) {
            val mid = (low + high) / 2.0
            if (simulateTrajectory(v, mid, pVel.y, horizontalDist).first < dy) low = mid else high = mid
        }
        return -low
    }

    // Pair<到達時のY座標, 到達ティック数> を返すように変更
    private fun simulateTrajectory(
        v: Double,
        pitchDeg: Double,
        initialVelY: Double,
        targetX: Double,
    ): Pair<Double, Int> {
        val pitchRad = pitchDeg * PI / 180.0
        var posX = 0.0
        var posY = 0.0
        var vX = v * cos(pitchRad)
        var vY = (v * sin(pitchRad)) + initialVelY

        for (tick in 1..maxStep) {
            posX += vX
            posY += vY
            vX *= drag
            vY = (vY * drag) - gravity
            if (posX >= targetX) return Pair(posY, tick)
        }
        return Pair(posY, maxStep)
    }

    data class PathResult(val status: PathStatus, val ticks: Int)

    protected fun verifyPath(
        v: Double,
        pVel: Vec3,
        xRot: Double,
        yRot: Double,
        target: Vec3,
        startPos: Vec3,
    ): PathResult {
        val level = level ?: return PathResult(PathStatus.Obstructed, 0)
        val player = player ?: return PathResult(PathStatus.Obstructed, 0)

        var currentPos = startPos
        val f = -sin(yRot * (PI / 180.0)) * cos(xRot * (PI / 180.0))
        val g = -sin(xRot * (PI / 180.0))
        val h = cos(yRot * (PI / 180.0)) * cos(xRot * (PI / 180.0))
        var velVec = Vec3(f, g, h).scale(v).add(pVel)

        for (tick in 1..maxStep) {
            val nextPos = currentPos.add(velVec)
            val result =
                level.clip(ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))

            if (result.type != HitResult.Type.MISS) {
                return if (result.location.distanceToSqr(target) < 3.0) {
                    PathResult(PathStatus.Clear, tick)
                } else {
                    PathResult(PathStatus.Obstructed, tick)
                }
            }

            currentPos = nextPos
            velVec = velVec.scale(drag).subtract(0.0, gravity, 0.0)

            if (currentPos.distanceToSqr(target) < 1.5) return PathResult(PathStatus.Clear, tick)
        }
        return PathResult(PathStatus.Uncertain, maxStep)
    }

    fun renderTrajectoryUI(

        graphics2D: Graphics2D,

        analysis: TrajectoryAnalysis,

        accentColor: Int,

        foregroundColor: Int,

    ): Graphics2D {
        val player = player ?: return graphics2D

        val screenPos = graphics2D.projectWorldToScreen(analysis.hitPos) ?: return graphics2D

        val x = screenPos.first.toFloat()

        val y = screenPos.second.toFloat()

        val distance = player.eyePosition.distanceTo(analysis.hitPos)

        val errorRadius = (distance.toFloat() * 0.2f / graphics2D.fovFactor).coerceIn(4f, 400f)

        graphics2D.beginPath()

        graphics2D.strokeStyle.color = when (analysis.status) {
            PathStatus.Obstructed -> accentColor.alpha(40)

            PathStatus.Uncertain -> accentColor.alpha(160)

            else -> accentColor.alpha(100)
        }

        graphics2D.strokeStyle.width = 1.2f

        graphics2D.arc(x, y, errorRadius, 0f, (PI * 2).toFloat())

        graphics2D.strokePath()

        val distText = String.format("%.1f m", distance)

        graphics2D.textStyle.size = 12f

        graphics2D.textStyle.font = "infinite_regular"

        graphics2D.fillStyle = foregroundColor
        val textDiff = errorRadius + graphics2D.textStyle.size
        if (y + textDiff >= graphics2D.height * 3 / 4f) {
            graphics2D.textCentered(distText, x, y - textDiff)
        } else if (y - textDiff <= graphics2D.height * 2 / 3f) {
            graphics2D.textCentered(distText, x, y + textDiff)
        } else {
            graphics2D.textCentered(distText, x, graphics2D.height / 3f)
        }
        return graphics2D
    }
}
