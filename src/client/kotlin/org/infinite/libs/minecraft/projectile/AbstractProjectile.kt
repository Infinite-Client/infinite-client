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
    open var drag: Double = 0.99
    abstract val precision: Int
    abstract val maxStep: Int

    fun analysisAdvanced(
        basePower: Double,
        target: Entity,
        startPos: Vec3,
        iterations: Int = 3,
        overrideTargetPos: Vec3? = null,
    ): TrajectoryAnalysis {
        val targetVel = target.deltaMovement
        val gameTimeDelta = minecraft.deltaTracker.gameTimeDeltaTicks

        // 初期ターゲット位置
        var currentPredictedPos = overrideTargetPos ?: target.getPosition(gameTimeDelta)
        var lastTicks = 0
        var finalAnalysis = TrajectoryAnalysis(0.0, 0.0, PathStatus.Uncertain, currentPredictedPos, 0, false)

        repeat(iterations) {
            // --- ターゲット位置の動的調整 (狙い目の決定) ---
            val finalTarget = if (overrideTargetPos != null) {
                overrideTargetPos
            } else {
                // 前回ループの ticks に基づいてオフセットを計算
                // 20t以内: 目 (eyeHeight)
                // 40t以内: 胴体 (eyeHeight * 0.5)
                // それ以上: 足元 (0.0)
                val heightOffset = when {
                    lastTicks <= 20 -> target.eyeHeight.toDouble()
                    lastTicks <= 40 -> target.eyeHeight.toDouble() * 0.5
                    else -> 0.0
                }

                // 元の予測位置（足元）に高さを加える
                currentPredictedPos.add(0.0, heightOffset, 0.0)
            }

            // 1. 現在の予測位置に対して解析を実行
            val currentAnalysis = analysisStaticPos(basePower, finalTarget, startPos)
            finalAnalysis = currentAnalysis
            lastTicks = currentAnalysis.travelTicks

            // 2. 射程外なら中断
            if (currentAnalysis.status == PathStatus.Unreachable) {
                return@repeat
            }

            // 3. 偏差予測（移動先）の更新
            val vY = if (target.onGround()) 0.0 else targetVel.y
            val basePos = overrideTargetPos ?: target.getPosition(gameTimeDelta)

            val nextPredictedPos = basePos.add(
                targetVel.x * lastTicks,
                vY * lastTicks,
                targetVel.z * lastTicks,
            )

            // 収束チェック
            if (nextPredictedPos.distanceToSqr(currentPredictedPos) < 0.01) {
                return@repeat
            }
            currentPredictedPos = nextPredictedPos
        }

        return finalAnalysis
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

        val maxRangePitch = findMaxRangeAngle(basePower, limitUpper, currentPitch)
        val (maxDist, _) = simulateForDistance(basePower, maxRangePitch, targetPos.y - startPos.y)

        if (horizontalDist > maxDist) {
            return TrajectoryAnalysis(maxRangePitch, yRot, PathStatus.Unreachable, targetPos, maxStep, false)
        }

        val highPitch = solvePitchStrict(basePower, targetPos, startPos, limitUpper, maxRangePitch)
        val lowPitch = solvePitchStrict(basePower, targetPos, startPos, maxRangePitch, currentPitch)

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

    protected fun simulateFast(v: Double, pitchDeg: Double, targetX: Double): Pair<Double, Int> {
        val rad = pitchDeg * (PI / 180.0)
        var pX = 0.0
        var pY = 0.0

        // --- 符号の修正ポイント ---
        // 1. velX: pitchが ±90° に近づくほど水平速度は 0 になるべきなので cos(rad) で正しい
        var velX = v * cos(rad)
        // 2. velY: Minecraftではマイナスが上なので、-sin(rad) とすることで
        //    上向き(-90°)の時に正の速度 (+1.0) が得られるようにする
        var velY = (-sin(rad) * v)

        var tick = 0
        while (tick < maxStep) {
            val distToTarget = targetX - pX
            val stepSize = when {
                distToTarget > 60.0 -> 5
                distToTarget > 20.0 -> 2
                else -> 1
            }.coerceAtMost(maxStep - tick)

            repeat(stepSize) {
                pX += velX
                pY += velY
                velX *= drag
                velY = (velY * drag) - gravity
            }

            tick += stepSize
            if (pX >= targetX) return Pair(pY, tick)
            // 下に落ちすぎた場合の早期終了
            if (velY < 0 && pY < -100.0) break
        }
        return Pair(pY, tick)
    }

    protected fun findMaxRangeAngle(v: Double, minP: Double, maxP: Double): Double {
        var low = minP
        var high = maxP
        repeat(10) {
            val m1 = low + (high - low) / 3
            val m2 = high - (high - low) / 3
            if (simulateForDistance(v, m1, 0.0).first > simulateForDistance(v, m2, 0.0).first) {
                high = m2
            } else {
                low = m1
            }
        }
        return (low + high) / 2.0
    }

    protected fun simulateForDistance(v: Double, pitchDeg: Double, targetDY: Double): Pair<Double, Int> {
        val rad = pitchDeg * PI / 180.0
        var pX = 0.0
        var pY = 0.0
        var velX = v * cos(rad)
        var velY = (-sin(rad) * v)

        for (tick in 1..maxStep) {
            pX += velX
            pY += velY
            velX *= drag
            velY = (velY * drag) - gravity
            if (velY < 0 && pY <= targetDY) return Pair(pX, tick)
        }
        return Pair(pX, maxStep)
    }

    protected fun solvePitchStrict(
        v: Double,
        target: Vec3,
        start: Vec3,
        minA: Double,
        maxA: Double,
    ): Double {
        val horizontalDist = sqrt((target.x - start.x).pow(2) + (target.z - start.z).pow(2))
        val targetDY = target.y - start.y
        var low = minA
        var high = maxA

        repeat(precision) {
            val mid = (low + high) * 0.5
            val (resY, _) = simulateFast(v, mid, horizontalDist)
            if (resY < targetDY) high = mid else low = mid
        }
        return (low + high) * 0.5
    }

    protected fun verifyPath(v: Double, xRot: Double, yRot: Double, target: Vec3, startPos: Vec3): PathResult {
        val level = level ?: return PathResult(PathStatus.Obstructed, 0)
        val player = player ?: return PathResult(PathStatus.Obstructed, 0)

        // Minecraftの角度(degrees)からラジアンへ
        val f = Math.toRadians(yRot)
        val g = Math.toRadians(xRot)

        // 方向ベクトルの算出 (Minecraft標準の数式)
        val hz = cos(g)
        val vx = -sin(f) * hz
        val vy = -sin(g)
        val vz = cos(f) * hz

        var velVec = Vec3(vx, vy, vz).normalize().scale(v)
        var currentPos = startPos
        for (tick in 1..maxStep) {
            val nextPos = currentPos.add(velVec)

            // 2ティックに1回だけクリップ判定を行う（大幅な負荷軽減、ただし細い鉄格子などは抜ける可能性あり）
            if (tick % 2 == 0 || currentPos.distanceToSqr(target) < 10.0) {
                val result = level.clip(
                    ClipContext(
                        currentPos,
                        nextPos,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player,
                    ),
                )
                if (result.type != HitResult.Type.MISS) {
                    return if (result.location.distanceToSqr(target) < 4.0) {
                        PathResult(PathStatus.Clear, tick)
                    } else {
                        PathResult(PathStatus.Obstructed, tick)
                    }
                }
            }

            currentPos = nextPos
            velVec = velVec.scale(drag).subtract(0.0, gravity, 0.0)
            if (currentPos.distanceToSqr(target) < 1.5) return PathResult(PathStatus.Clear, tick)
        }
        return PathResult(PathStatus.Uncertain, maxStep)
    }

    // --- UI描画ロジック ---
    fun renderTrajectoryUI(
        graphics2D: Graphics2D,
        analysis: TrajectoryAnalysis,
        accentColor: Int,
        foregroundColor: Int,
    ): Graphics2D {
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
