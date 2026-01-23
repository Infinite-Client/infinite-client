package org.infinite.libs.minecraft.aim.task

import net.minecraft.client.player.LocalPlayer
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.graphics3d.structs.CameraRoll
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.minecraft.aim.task.condition.AimTaskConditionInterface
import org.infinite.libs.minecraft.aim.task.condition.AimTaskConditionReturn
import org.infinite.libs.minecraft.aim.task.config.AimCalculateMethod
import org.infinite.libs.minecraft.aim.task.config.AimPriority
import org.infinite.libs.minecraft.aim.task.config.AimProcessResult
import org.infinite.libs.minecraft.aim.task.config.AimTarget
import kotlin.math.sqrt

open class AimTask(
    open val priority: AimPriority,
    open val target: AimTarget,
    open val condition: AimTaskConditionInterface,
    open val calcMethod: AimCalculateMethod,
    open val multiply: Double = 1.0,
    val onSuccess: () -> Unit = {},
    val onFailure: () -> Unit = {},
) : MinecraftInterface() {
    private fun mouseSensitivity(): Double = options.sensitivity().get()
    private var rollVelocity = CameraRoll(0.0, 0.0)
    private var duration = (1000 / 30).toLong()
    private var time = System.currentTimeMillis()

    open fun atSuccess() {
        onSuccess()
    }

    open fun atFailure() {
        onFailure()
    }

    /**
     * AimTaskを1ティック（フレーム）処理します。
     * @return エイム処理の結果
     */
    fun process(): AimProcessResult {
        val currentTime = System.currentTimeMillis()
        duration = currentTime - time
        time = currentTime
        val condition = condition.check()
        val player = minecraft.player ?: return AimProcessResult.Failure
        val targetPos = target.pos() ?: return AimProcessResult.Failure
        when (condition) {
            AimTaskConditionReturn.Success -> {
                return AimProcessResult.Success
            }

            AimTaskConditionReturn.Failure -> {
                return AimProcessResult.Failure
            }

            AimTaskConditionReturn.Suspend -> {
                return AimProcessResult.Progress
            }

            AimTaskConditionReturn.Exec -> {
                val rollDiff =
                    if (target is AimTarget.RollTarget) {
                        ((target as AimTarget.RollTarget).roll - playerRoll(player)).diffNormalize()
                    } else {
                        calculateRotation(player, targetPos)
                    }
                rollVelocity = calcExecRotation(rollDiff, calcMethod)
                rollAim(player, rollVelocity)
                return AimProcessResult.Progress
            }

            AimTaskConditionReturn.Force -> {
                val targetRoll = calcLookAt(targetPos)
                setAim(player, targetRoll)
                return AimProcessResult.Success
            }
        }
    }

    private fun calcExecRotation(
        rollDiff: CameraRoll,
        calcMethod: AimCalculateMethod,
    ): CameraRoll {
        // 【修正点2】マウス感度にmultiply値を乗算し、エイム速度を調整する
        val scaledSensitivity = (mouseSensitivity().coerceAtLeast(0.1)) * multiply

        // 【修正】すべてのMethodで基準となる最大移動速度を定義する（Linearのscaler=10を基準とする）
        val baseMaxSpeed = duration * scaledSensitivity

        val result =
            when (calcMethod) {
                AimCalculateMethod.EaseOut -> {
                    val scaler = 50
                    val diffMultiply = (duration * scaledSensitivity / scaler).coerceAtMost(1.0)

                    // ただし、diffMultiplyが1.0未満の時のみ徐々に減速する挙動を維持
                    if (diffMultiply < 1.0) {
                        rollDiff * diffMultiply
                    } else {
                        // diffMultiplyが1.0以上の場合は、目標移動量そのまま（即座に完了）
                        rollDiff
                    }
                }

                AimCalculateMethod.Linear -> {
                    // 【変更なし】baseMaxSpeedを使用
                    rollDiff.limitedBySpeed(baseMaxSpeed)
                }

                AimCalculateMethod.EaseIn -> {
                    // 【修正】加速度(acceleration)をbaseMaxSpeedに依存させて統一的な速度基準とする
                    val currentMagnitude = rollVelocity.magnitude()
                    val targetMagnitude = rollDiff.magnitude()

                    val acceleration = baseMaxSpeed / 2

                    if (currentMagnitude < targetMagnitude) {
                        rollDiff.limitedBySpeed(currentMagnitude + acceleration)
                    } else {
                        rollDiff
                    }
                }

                AimCalculateMethod.EaseInOut -> {
                    // ロジックは変更せず、EaseInとEaseOutの統一された基準に基づいて計算される
                    val easeIn = calcExecRotation(rollDiff, AimCalculateMethod.EaseIn)
                    val easeOut = calcExecRotation(rollDiff, AimCalculateMethod.EaseOut)
                    val easeInMagnitude = easeIn.magnitude()
                    val easeOutMagnitude = easeOut.magnitude()
                    if (easeOutMagnitude > easeInMagnitude) {
                        easeIn
                    } else {
                        easeOut
                    }
                }

                AimCalculateMethod.Immediate -> {
                    rollDiff
                }
            }
        return result.diffNormalize()
    }

    private fun rollAim(
        player: LocalPlayer,
        roll: CameraRoll,
    ) {
        val yRot = player.yRot
        val xRot = player.xRot
        setAim(player, CameraRoll(yRot + roll.yRot, xRot + roll.xRot))
    }

    companion object : MinecraftInterface() {
        fun calcLookAt(target: Vec3): CameraRoll {
            val player = player ?: return CameraRoll.Zero

            // 【修正】足元ではなく目の位置(EyePos)を基準に計算する
            val eyePos: Vec3 = player.eyePosition

            val d = target.x - eyePos.x
            val e = target.y - eyePos.y
            val f = target.z - eyePos.z
            val g = sqrt(d * d + f * f)

            val pitch = Mth.wrapDegrees(-(Mth.atan2(e, g) * (180.0 / Math.PI)))
            val yaw = Mth.wrapDegrees((Mth.atan2(f, d) * (180.0 / Math.PI)) - 90.0)

            // CameraRoll(yaw, pitch) の順序をプロジェクトの定義に合わせる
            return CameraRoll(yaw, pitch)
        }
    }

// ...

    private fun calculateRotation(player: LocalPlayer, targetPos: Vec3): CameraRoll {
        val t = calcLookAt(targetPos)
        val c = playerRoll(player)
        // 正規化された差分を返す
        return (t - c).diffNormalize()
    }

    private fun playerRoll(player: LocalPlayer): CameraRoll = // 【修正】引数の順序を (Yaw, Pitch) に統一
        CameraRoll(player.yRot.toDouble(), player.xRot.toDouble())

    private fun setAim(player: LocalPlayer, roll: CameraRoll) {
        // 【修正】roll.yRot が Yaw、roll.xRot が Pitch であることを確認
        player.yRot = roll.yRot.toFloat()
        player.xRot = roll.xRot.toFloat()
    }
}
