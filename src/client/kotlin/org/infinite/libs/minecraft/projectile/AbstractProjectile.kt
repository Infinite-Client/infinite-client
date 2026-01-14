package org.infinite.libs.minecraft.projectile

import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.utils.alpha
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

abstract class AbstractProjectile : MinecraftInterface() {
    enum class PathStatus { Clear, Obstructed, Uncertain, Unreachable }
    data class TrajectoryAnalysis(
        val xRot: Double,
        val yRot: Double,
        val status: PathStatus,
        val hitPos: Vec3,
        val travelTicks: Int,
        val isHighArc: Boolean,
    )

    abstract val gravity: Double
    abstract val drag: Double
    abstract val precision: Int
    abstract val maxStep: Int

    fun analysisAdvanced(
        basePower: Double,
        target: Entity,
        startPos: Vec3,
        iterations: Int = 3,
    ): TrajectoryAnalysis {
        val targetVel = target.deltaMovement
        var predictedPos = target.getPosition(minecraft.deltaTracker.gameTimeDeltaTicks)
        var lastAnalysis = TrajectoryAnalysis(0.0, 0.0, PathStatus.Uncertain, predictedPos, 0, false)

        repeat(iterations) {
            val dx = predictedPos.x - startPos.x
            val dz = predictedPos.z - startPos.z
            val horizontalDist = sqrt(dx * dx + dz * dz)
            val player = player ?: return@repeat

            val yRot = if (horizontalDist < 0.0001) player.yRot.toDouble() else (atan2(-dx, dz) * (180.0 / PI))
            val currentPitch = player.xRot.toDouble()
            val limitUpper = currentPitch - 90.0

            // 最大射程の計算 (playerVel.y は 0.0 として扱う)
            val maxRangePitch = findMaxRangeAngle(basePower, 0.0, limitUpper, currentPitch)
            val (maxDist, _) = simulateForDistance(basePower, maxRangePitch, 0.0, predictedPos.y - startPos.y)

            if (horizontalDist > maxDist) {
                lastAnalysis = TrajectoryAnalysis(maxRangePitch, yRot, PathStatus.Unreachable, predictedPos, maxStep, false)
            } else {
                val highPitch = solvePitchStrict(basePower, 0.0, predictedPos, startPos, limitUpper, maxRangePitch)
                val lowPitch = solvePitchStrict(basePower, 0.0, predictedPos, startPos, maxRangePitch, currentPitch)

                // --- ターゲット位置の動的調整 ---
                // 近距離（低射角と現在の視線の差が小さい）かどうかの判定
                val isCloseRange = kotlin.math.abs(lowPitch - currentPitch) < 5.0

                val targetOffset = if (isCloseRange) {
                    Vec3(0.0, target.eyeHeight.toDouble(), 0.0)
                } else {
                    // 遠距離: 足元（下側）の少し手前を狙う
                    val toPlayer = startPos.subtract(predictedPos).normalize().scale(0.2)
                    Vec3(toPlayer.x, target.eyeHeight * 0.1, toPlayer.z)
                }

                val finalTarget = predictedPos.add(targetOffset)

                // 地形チェック
                val highRes = verifyPath(basePower, highPitch, yRot, finalTarget, startPos)
                val lowRes = verifyPath(basePower, lowPitch, yRot, finalTarget, startPos)

                lastAnalysis = when {
                    lowRes.status == PathStatus.Clear ->
                        TrajectoryAnalysis(lowPitch, yRot, PathStatus.Clear, finalTarget, lowRes.ticks, false)
                    highRes.status == PathStatus.Clear ->
                        TrajectoryAnalysis(highPitch, yRot, PathStatus.Clear, finalTarget, highRes.ticks, true)
                    else ->
                        TrajectoryAnalysis(highPitch, yRot, PathStatus.Obstructed, finalTarget, highRes.ticks, true)
                }
            }

            // 偏差予測の更新
            val ticks = lastAnalysis.travelTicks
            val vY = if (target.onGround()) 0.0 else targetVel.y
            predictedPos = target.getPosition(minecraft.deltaTracker.gameTimeDeltaTicks).add(
                targetVel.x * ticks,
                vY * ticks,
                targetVel.z * ticks,
            )
        }
        return lastAnalysis
    }

    fun analysisStaticPos(
        basePower: Double,
        targetPos: Vec3,
        startPos: Vec3,
    ): TrajectoryAnalysis {
        val dx = targetPos.x - startPos.x
        val dz = targetPos.z - startPos.z
        val horizontalDist = sqrt(dx * dx + dz * dz)
        val player = player ?: return TrajectoryAnalysis(0.0, 0.0, PathStatus.Uncertain, targetPos, 0, false)

        val yRot = if (horizontalDist < 0.0001) player.yRot.toDouble() else (atan2(-dx, dz) * (180.0 / PI))
        val currentPitch = player.xRot.toDouble()
        val limitUpper = currentPitch - 90.0

        val maxRangePitch = findMaxRangeAngle(basePower, 0.0, limitUpper, currentPitch)
        val (maxDist, _) = simulateForDistance(basePower, maxRangePitch, 0.0, targetPos.y - startPos.y)

        if (horizontalDist > maxDist) {
            return TrajectoryAnalysis(maxRangePitch, yRot, PathStatus.Unreachable, targetPos, maxStep, false)
        }

        val highPitch = solvePitchStrict(basePower, 0.0, targetPos, startPos, limitUpper, maxRangePitch)
        val lowPitch = solvePitchStrict(basePower, 0.0, targetPos, startPos, maxRangePitch, currentPitch)

        val highRes = verifyPath(basePower, highPitch, yRot, targetPos, startPos)
        val lowRes = verifyPath(basePower, lowPitch, yRot, targetPos, startPos)

        return when {
            lowRes.status == PathStatus.Clear ->
                TrajectoryAnalysis(lowPitch, yRot, PathStatus.Clear, targetPos, lowRes.ticks, false)
            highRes.status == PathStatus.Clear ->
                TrajectoryAnalysis(highPitch, yRot, PathStatus.Clear, targetPos, highRes.ticks, true)
            else ->
                TrajectoryAnalysis(highPitch, yRot, PathStatus.Obstructed, targetPos, highRes.ticks, true)
        }
    }

    // --- 内部計算メソッド (playerVelを削除) ---

    protected fun findMaxRangeAngle(v: Double, vY0: Double, minP: Double, maxP: Double): Double {
        var low = minP
        var high = maxP
        repeat(10) {
            val m1 = low + (high - low) / 3
            val m2 = high - (high - low) / 3
            if (simulateForDistance(v, m1, vY0, 0.0).first > simulateForDistance(v, m2, vY0, 0.0).first) {
                high = m2
            } else {
                low = m1
            }
        }
        return (low + high) / 2.0
    }

    protected fun simulateForDistance(v: Double, pitchDeg: Double, vY0: Double, targetDY: Double): Pair<Double, Int> {
        val rad = pitchDeg * PI / 180.0
        var pX = 0.0
        var pY = 0.0
        var velX = v * cos(rad)
        var velY = (-sin(rad) * v) + vY0

        for (tick in 1..maxStep) {
            pX += velX
            pY += velY
            velX *= drag
            velY = (velY * drag) - gravity
            if (velY < 0 && pY <= targetDY) return Pair(pX, tick)
        }
        return Pair(pX, maxStep)
    }

    protected fun simulateForYAtDistance(v: Double, pitchDeg: Double, vY0: Double, targetX: Double): Pair<Double, Int> {
        val rad = pitchDeg * PI / 180.0
        var pX = 0.0
        var pY = 0.0
        var velX = v * cos(rad)
        var velY = (-sin(rad) * v) + vY0

        for (tick in 1..maxStep) {
            pX += velX
            pY += velY
            velX *= drag
            velY = (velY * drag) - gravity
            if (pX >= targetX) return Pair(pY, tick)
        }
        return Pair(pY, maxStep)
    }

    protected fun solvePitchStrict(v: Double, vY0: Double, target: Vec3, start: Vec3, minA: Double, maxA: Double): Double {
        val horizontalDist = sqrt((target.x - start.x).pow(2) + (target.z - start.z).pow(2))
        val targetDY = target.y - start.y
        var low = minA
        var high = maxA
        repeat(precision) {
            val mid = (low + high) / 2.0
            val (resY, _) = simulateForYAtDistance(v, mid, vY0, horizontalDist)
            if (resY < targetDY) high = mid else low = mid
        }
        return (low + high) / 2.0
    }

    protected fun verifyPath(v: Double, xRot: Double, yRot: Double, target: Vec3, startPos: Vec3): PathResult {
        val level = level ?: return PathResult(PathStatus.Obstructed, 0)
        val player = player ?: return PathResult(PathStatus.Obstructed, 0)

        var currentPos = startPos
        val f = -sin(yRot * (PI / 180.0)) * cos(xRot * (PI / 180.0))
        val g = -sin(xRot * (PI / 180.0))
        val h = cos(yRot * (PI / 180.0)) * cos(xRot * (PI / 180.0))
        var velVec = Vec3(f, g, h).scale(v) // playerVelを除外

        for (tick in 1..maxStep) {
            val nextPos = currentPos.add(velVec)
            val result = level.clip(ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))

            if (result.type != HitResult.Type.MISS) {
                return if (result.location.distanceToSqr(target) < 4.0) {
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

    // --- UI描画ロジック ---
    fun renderTrajectoryUI(graphics2D: Graphics2D, analysis: TrajectoryAnalysis, accentColor: Int, foregroundColor: Int): Graphics2D {
        val player = player ?: return graphics2D
        val screenPos = graphics2D.projectWorldToScreen(analysis.hitPos) ?: return graphics2D
        val colorScheme = InfiniteClient.theme.colorScheme

        val x = screenPos.first.toFloat()
        val y = screenPos.second.toFloat()
        val distance = player.eyePosition.distanceTo(analysis.hitPos)

        val baseRadius = (distance.toFloat() * 0.15f / graphics2D.fovFactor).coerceIn(3f, 100f)

        graphics2D.beginPath()
        graphics2D.strokeStyle.width = if (analysis.isHighArc) 1.0f else 2.0f
        graphics2D.strokeStyle.color = when (analysis.status) {
            PathStatus.Obstructed -> accentColor.alpha(60)
            PathStatus.Unreachable -> colorScheme.color(30f, 1f, 0.5f).alpha(150)
            else -> accentColor.alpha(200)
        }

        if (analysis.isHighArc) {
            graphics2D.arc(x, y, baseRadius, 0f, (PI * 2).toFloat())
            graphics2D.strokePath()
            graphics2D.beginPath()
            graphics2D.arc(x, y, baseRadius * 0.6f, 0f, (PI * 2).toFloat())
        } else {
            graphics2D.arc(x, y, baseRadius, 0f, (PI * 2).toFloat())
        }
        graphics2D.strokePath()

        val centerX = graphics2D.width / 2f
        val centerY = graphics2D.height / 2f
        val infoY = centerY - 45f

        val modeText = if (analysis.isHighArc) "HIGH ARC" else "LOW ARC"
        val distText = String.format("%.1f m (%dt)", distance, analysis.travelTicks)
        val statusText = when (analysis.status) {
            PathStatus.Clear -> "● CLEAR"
            PathStatus.Obstructed -> "× OBSTRUCTED"
            PathStatus.Unreachable -> "！ OUT OF RANGE"
            else -> "? UNCERTAIN"
        }

        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.textStyle.size = 9f
        graphics2D.fillStyle = accentColor.alpha(160)
        graphics2D.textCentered(modeText, centerX, infoY - 14f)

        graphics2D.textStyle.size = 13f
        graphics2D.fillStyle = foregroundColor
        graphics2D.textCentered(distText, centerX, infoY)

        graphics2D.textStyle.size = 9f
        graphics2D.fillStyle = when (analysis.status) {
            PathStatus.Clear -> colorScheme.greenColor
            PathStatus.Unreachable -> colorScheme.color(30f, 1f, 0.5f)
            else -> colorScheme.redColor
        }
        graphics2D.textCentered(statusText, centerX, infoY + 11f)

        return graphics2D
    }

    data class PathResult(val status: PathStatus, val ticks: Int)
}
