package org.infinite.libs.minecraft.projectile

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.rust.projectile.ProjectileEmulator
import org.infinite.utils.alpha
import kotlin.math.*

abstract class AbstractProjectile : MinecraftInterface() {
    enum class PathStatus { Clear, Obstructed, Uncertain, Unreachable }
    data class TrajectoryAnalysis(
        val xRot: Double,
        val yRot: Double,
        val status: PathStatus,
        val hitPos: Vec3,
        val travelTicks: Int,
        val isHighArc: Boolean,
        val power: Double,
    )

    abstract val gravity: Double
    open var drag: Double = 0.99
    abstract val precision: Int
    abstract val maxStep: Int

    fun analysisAdvanced(
        basePower: Double,
        target: Entity,
        startPos: Vec3,
        iterations: Int = 5,
        overrideTargetPos: Vec3? = null,
    ): TrajectoryAnalysis {
        val vel = target.deltaMovement
        val targetPos = overrideTargetPos ?: target.position()
        val targetGrav = if (target.isNoGravity) 0f else 0.08f

        // 1. 本来のターゲット（ロックオン対象等）に対する高度な解析
        val res = ProjectileEmulator.analyzeAdvanced(
            basePower.toFloat(), startPos, targetPos, vel,
            drag.toFloat(), gravity.toFloat(), targetGrav,
            precision, maxStep, iterations,
        )

        val dx = res.predictedPos.x - startPos.x
        val dz = res.predictedPos.z - startPos.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        // 射程内かつ障害物がないかチェック
        var finalAnalysis: TrajectoryAnalysis? = null

        if (horizontalDist <= res.maxDist) {
            // 低角の検証
            val lowRes = verifyPath(basePower, res.lowP.toDouble(), res.yaw.toDouble(), res.predictedPos, startPos)
            if (lowRes.status == PathStatus.Clear) {
                finalAnalysis = TrajectoryAnalysis(res.lowP.toDouble(), res.yaw.toDouble(), PathStatus.Clear, res.predictedPos, res.ticks, false, basePower)
            } else {
                // 高角の検証
                val highRes = verifyPath(basePower, res.highP.toDouble(), res.yaw.toDouble(), res.predictedPos, startPos)
                if (highRes.status == PathStatus.Clear) {
                    finalAnalysis = TrajectoryAnalysis(res.highP.toDouble(), res.yaw.toDouble(), PathStatus.Clear, res.predictedPos, res.ticks, true, basePower)
                }
            }
        }

        // 2. どちらもダメ（射程外 or 障害物あり）な場合、目の前の対象にターゲットを変更
        if (finalAnalysis == null) {
            // LockOn中の機能等から reach を取得するか、デフォルト値（例: 100m）を使用
            val reach = 100.0
            val fallbackTarget = getTargetPos(reach)

            return if (fallbackTarget != null) {
                // 目の前の位置（静止座標）をターゲットとして再計算
                analysisStaticPos(basePower, fallbackTarget, startPos)
            } else {
                // フォールバック先すら見つからない場合は、仕方なく元の「Obstructed」な結果を返す
                TrajectoryAnalysis(res.lowP.toDouble(), res.yaw.toDouble(), PathStatus.Obstructed, res.predictedPos, res.ticks, false, basePower)
            }
        }

        return finalAnalysis
    }

    /**
     * 静止している特定の座標に対して弾道を解析します。
     */
    fun analysisStaticPos(
        basePower: Double,
        targetPos: Vec3,
        startPos: Vec3,
    ): TrajectoryAnalysis {
        // 静止ターゲットとして、速度 0, ターゲット重力 0 で Rust を呼び出す
        val res = ProjectileEmulator.analyzeAdvanced(
            basePower.toFloat(), startPos, targetPos, Vec3.ZERO,
            drag.toFloat(), gravity.toFloat(), 0f,
            precision, maxStep, 1, // 静止しているのでイテレーションは1回で十分
        )

        val dx = targetPos.x - startPos.x
        val dz = targetPos.z - startPos.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        // 射程外チェック
        if (horizontalDist > res.maxDist) {
            return TrajectoryAnalysis(res.lowP.toDouble(), res.yaw.toDouble(), PathStatus.Unreachable, targetPos, maxStep, false, basePower)
        }

        // 障害物判定
        val lowRes = verifyPath(basePower, res.lowP.toDouble(), res.yaw.toDouble(), targetPos, startPos)
        if (lowRes.status == PathStatus.Clear) {
            return TrajectoryAnalysis(res.lowP.toDouble(), res.yaw.toDouble(), PathStatus.Clear, targetPos, res.ticks, false, basePower)
        }

        val highRes = verifyPath(basePower, res.highP.toDouble(), res.yaw.toDouble(), targetPos, startPos)
        return TrajectoryAnalysis(
            res.highP.toDouble(),
            res.yaw.toDouble(),
            if (highRes.status == PathStatus.Clear) PathStatus.Clear else PathStatus.Obstructed,
            targetPos,
            res.ticks,
            true,
            basePower,
        )
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

        val x = screenPos.first
        val y = screenPos.second
        val distance = player.eyePosition.distanceTo(analysis.hitPos)

        val baseRadius = (distance.toFloat() * 0.15f / graphics2D.fovFactor).coerceIn(3f, 100f)

        // Draw the path
        drawTrajectoryPath(graphics2D, analysis, accentColor)

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

    private fun drawTrajectoryPath(graphics2D: Graphics2D, analysis: TrajectoryAnalysis, color: Int) {
        val player = player ?: return
        val startPos = player.getEyePosition(1.0f)
        val isHit = analysis.status == PathStatus.Clear || analysis.status == PathStatus.Obstructed

        // --- 1. 軌道線の描画 ---
        graphics2D.beginPath()
        graphics2D.strokeStyle.enabledGradient = true // strokeStyleに直接設定
        graphics2D.strokeStyle.width = 1.5f

        val f = Math.toRadians(analysis.yRot)
        val g = Math.toRadians(analysis.xRot)
        val hz = cos(g)
        val vx = -sin(f) * hz
        val vy = -sin(g)
        val vz = cos(f) * hz

        var velVec = Vec3(vx, vy, vz).normalize().scale(analysis.power)
        var currentPos = startPos

        val totalTicks = analysis.travelTicks
        val fadeTicks = 10

        for (tick in 0..totalTicks) {
            val screenPos = graphics2D.projectWorldToScreen(currentPos)
            var alphaMultiplier = 1.0f
            if (tick < fadeTicks) {
                alphaMultiplier = tick.toFloat() / fadeTicks
            } else if (!isHit && tick > totalTicks - fadeTicks) {
                alphaMultiplier = (totalTicks - tick).toFloat() / fadeTicks
            }

            // グラデーション有効時は一画ごとに色が設定される
            graphics2D.strokeStyle.color = color.alpha((120 * alphaMultiplier).toInt())

            if (screenPos != null) {
                if (tick == 0) {
                    graphics2D.moveTo(screenPos.first, screenPos.second)
                } else {
                    graphics2D.lineTo(screenPos.first, screenPos.second)
                }
            }

            currentPos = currentPos.add(velVec)
            velVec = velVec.scale(drag).subtract(0.0, gravity, 0.0)
        }
        graphics2D.strokePath()
        graphics2D.strokeStyle.enabledGradient = false

        // --- 2. 着弾地点の誤差円の描画 ---
        if (isHit) {
            val hitScreenPos = graphics2D.projectWorldToScreen(analysis.hitPos)
            if (hitScreenPos != null) {
                val distance = player.eyePosition.distanceTo(analysis.hitPos).coerceAtLeast(1.0)

                // 半径の計算: 0.5m の範囲を画面上に投影
                // fovFactor は FOVの変化に応じたスケール。基本は 1.0
                val errorRadiusWorld = 0.5f
                val scaleFactor = (graphics2D.height / 2.0f) / tan(Math.toRadians(graphics2D.fovFactor.toDouble() * 35.0)).toFloat()
                val errorRadiusScreen = (errorRadiusWorld * scaleFactor / distance.toFloat())

                graphics2D.beginPath() // 軌道線のパスをクリア
                graphics2D.strokeStyle.color = color.alpha(180)
                graphics2D.strokeStyle.width = 1.2f
                graphics2D.strokeStyle.enabledGradient = false // 円は単色

                graphics2D.arc(
                    hitScreenPos.first,
                    hitScreenPos.second,
                    errorRadiusScreen.coerceIn(4f, 200f),
                    0f,
                    (PI * 2).toFloat(),
                )
                graphics2D.strokePath()
            }
        }
    }
    protected fun getTargetPos(reach: Double): Vec3? {
        val p = player ?: return null
        // エンティティを含めたレイキャスト。MISSなら射程限界の空中をターゲットにする
        val hitResult = ProjectileUtil.getHitResultOnViewVector(p, { !it.isSpectator && it.isPickable }, reach)
        return if (hitResult.type != HitResult.Type.MISS) {
            hitResult.location
        } else {
            p.getEyePosition(1.0f).add(p.lookAngle.normalize().scale(reach))
        }
    }
    data class PathResult(val status: PathStatus, val ticks: Int)
}
