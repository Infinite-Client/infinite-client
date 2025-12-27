package org.infinite.libs.client.aim.task

import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.infinite.libs.client.aim.camera.CameraRoll
import org.infinite.libs.client.aim.task.condition.AimTaskConditionInterface
import org.infinite.libs.client.aim.task.condition.AimTaskConditionReturn
import org.infinite.libs.client.aim.task.config.AimCalculateMethod
import org.infinite.libs.client.aim.task.config.AimPriority
import org.infinite.libs.client.aim.task.config.AimProcessResult
import org.infinite.libs.client.aim.task.config.AimTarget
import kotlin.math.sqrt

open class AimTask(
    open val priority: AimPriority,
    open val target: AimTarget,
    open val condition: AimTaskConditionInterface,
    open val calcMethod: AimCalculateMethod,
    open val multiply: Double = 1.0,
    val onSuccess: () -> Unit = {},
    val onFailure: () -> Unit = {},
) {
    private fun mouseSensitivity(): Double = Minecraft
        .getInstance()
        .options
        .sensitivity()
        .get()

    companion object {
        fun calcLookAt(target: Vec3): CameraRoll {
            val vec3d: Vec3 = Minecraft.getInstance().player!!.eyePosition
            val d = target.x - vec3d.x
            val e = target.y - vec3d.y
            val f = target.z - vec3d.z
            val g = sqrt(d * d + f * f)
            val pitch =
                Mth.wrapDegrees(
                    (
                        -(
                            Mth.atan2(
                                e,
                                g,
                            ) * (180.0 / Math.PI)
                            )
                        ),
                )
            val yaw =
                Mth.wrapDegrees(
                    (
                        Mth.atan2(
                            f,
                            d,
                        ) * (180.0 / Math.PI)
                        ) - 90.0,
                )
            return CameraRoll(yaw, pitch)
        }
    }

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
     * @param client MinecraftClientインスタンス
     * @return エイム処理の結果
     */
    fun process(client: Minecraft): AimProcessResult {
        val currentTime = System.currentTimeMillis()
        duration = currentTime - time
        time = currentTime
        val condition = condition.check()
        val player = client.player ?: return AimProcessResult.Failure
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
        val baseMaxSpeed = (duration * scaledSensitivity / 10)

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

    /**
     * プレイヤーの現在の視線と目標座標から必要な回転量を計算します。
     * @return (Target Yaw, Target Pitch)
     */
    private fun calculateRotation(
        player: LocalPlayer,
        targetPos: Vec3,
    ): CameraRoll {
        val t = calcLookAt(targetPos)
        val c = playerRoll(player)
        return (t - c).diffNormalize()
    }

    private fun playerRoll(player: LocalPlayer): CameraRoll = CameraRoll(player.yRot.toDouble(), player.xRot.toDouble())

    /**
     * 進行度に基づき、開始角度から目標角度へプレイヤーの視線を補間・設定します。
     */
    private fun setAim(
        player: LocalPlayer,
        roll: CameraRoll,
    ) {
        player.setYRot(roll.yaw.toFloat())
        player.setXRot(roll.pitch.toFloat())
    }

    private fun rollAim(
        player: LocalPlayer,
        roll: CameraRoll,
    ) {
        val currentYaw = player.yRot
        val currentPitch = player.xRot
        setAim(player, CameraRoll(currentYaw + roll.yaw, currentPitch + roll.pitch))
    }
}
