package org.infinite.features.server.anti

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.effect.StatusEffects
import org.infinite.InfiniteClient
import org.infinite.feature.ConfigurableFeature
import org.infinite.libs.client.player.effectLevel
import org.infinite.libs.graphics.Graphics2D
import org.infinite.settings.FeatureSetting
import kotlin.math.sqrt

// 未使用のFeatureSettingを削除し、クラスをクリーンアップ
class AntiVulcan : ConfigurableFeature(initialEnabled = true) {
    private val debugHudSetting = FeatureSetting.BooleanSetting("DebugHud", false)
    override val settings: List<FeatureSetting<*>> = listOf(debugHudSetting)

    // airTicksをクラス変数として保持
    private var airTicks = 0

    // --- 定数（閾値）の定義 ---
    private val maxSprintSpeedOnGround = 0.32
    private val maxSpeedInAir = 0.45
    private val maxAscentSpeed = 0.45
    private val maxTerminalVelocity = 4.0
    private val maxNormalAirTicks = 40

    // 誤検知対策: 許容範囲を調整
    private val groundSpoofVerticalDeltaTolerance = 0.0001

    // --- 検出結果の構造化 (内部クラス) ---
    sealed class IllegalState(
        open val name: String,
        open val violationLevel: Double, // 違反度合いを示す値 (VL)
        open val description: String,
    ) {
        class SpeedIllegalState(
            override val violationLevel: Double,
        ) : IllegalState(
                name = "Speed",
                violationLevel = violationLevel,
                description = "水平速度が許容範囲を超過しています。",
            )

        class FlyIllegalState(
            override val violationLevel: Double,
            reason: String,
        ) : IllegalState(
                name = "Fly",
                violationLevel = violationLevel,
                description = "垂直方向または滞空時間に異常な挙動が見られます。理由: $reason",
            )

        class GroundSpoofIllegalState(
            override val violationLevel: Double,
            reason: String,
        ) : IllegalState(
                name = "GroundSpoof",
                violationLevel = violationLevel,
                description = "onGroundフラグがサーバー側の物理挙動と一致しません。理由: $reason",
            )
    }

    // --- メイン検出関数 ---

    fun detect(player: ClientPlayerEntity): List<IllegalState> {
        val illegalStates = mutableListOf<IllegalState>()

        val velocity = player.velocity
        val horizontalSpeed = sqrt(velocity.x * velocity.x + velocity.z * velocity.z)
        val verticalSpeed = velocity.y

        // ------------------------------------
        // 1. SPEED チート検出
        // ------------------------------------
        var maxHorizontal =
            if (player.isOnGround) {
                if (player.isSprinting) maxSprintSpeedOnGround else 0.23
            } else {
                maxSpeedInAir
            }

        val speedLevel = player.effectLevel(StatusEffects.SPEED)
        maxHorizontal *= 1.0 + (speedLevel * 0.2)

        if (horizontalSpeed > maxHorizontal) {
            horizontalSpeed - maxHorizontal
            val speedVL = (horizontalSpeed / maxHorizontal - 1.0) * 100.0
            illegalStates.add(
                IllegalState.SpeedIllegalState(speedVL),
            )
        }

        // ------------------------------------
        // 2. FLY チート検出 (垂直方向と滞空時間)
        // ------------------------------------

        // A. 異常な滞空時間 (AirTime)
        if (!player.isOnGround && airTicks > maxNormalAirTicks) {
            val exceedingTicks = (airTicks - maxNormalAirTicks).toDouble()
            val airTimeVL = exceedingTicks * 2.5
            illegalStates.add(
                IllegalState.FlyIllegalState(airTimeVL, "AirTime"),
            )
        }

        // B. 異常な上昇速度 (Ascend/Anti-Gravity)
        if (verticalSpeed > 0.01) { // 上昇中
            if (verticalSpeed > maxAscentSpeed) {
                val ascendVL = (verticalSpeed / maxAscentSpeed - 1.0) * 200.0
                illegalStates.add(
                    IllegalState.FlyIllegalState(ascendVL, "Ascend"),
                )
            }
        }

        // C. 異常な落下速度 (Terminal Velocity)
        if (verticalSpeed < 0.0) { // 落下中
            val actualFallSpeed = kotlin.math.abs(verticalSpeed)
            if (actualFallSpeed > maxTerminalVelocity) {
                val terminalVL = (actualFallSpeed / maxTerminalVelocity - 1.0) * 150.0
                illegalStates.add(
                    IllegalState.FlyIllegalState(terminalVL, "TerminalVelocity"),
                )
            }
        }

        // ------------------------------------
        // 3. GROUND SPOOF (地面偽装) チート検出
        // ------------------------------------

        val yDelta = kotlin.math.abs(player.y - player.lastY)

        // A. Y座標の不自然な変動 (Y_Mismatch)
        // 着地時の誤検知対策: airTicks > 0 の条件は不要。onGround=trueならば常に静止しているべき。
        if (player.isOnGround && yDelta > groundSpoofVerticalDeltaTolerance) {
            // 垂直速度が静止に近いはずなのに、y座標が動いている矛盾を検出
            val mismatchVL = (yDelta / groundSpoofVerticalDeltaTolerance) * 10.0

            illegalStates.add(
                IllegalState.GroundSpoofIllegalState(mismatchVL, "Y_Mismatch"),
            )
        }

        // B. 落下速度隠蔽 (HiddenVelocity)
        // プレイヤーがonGround=falseだが、Yの変動が極めて小さい
        // 着地した瞬間はyDeltaが極めて小さくなる可能性があるため、airTicks>1の条件を復活させ、着地ティックを無視する
        if (!player.isOnGround && airTicks > 1 && yDelta < groundSpoofVerticalDeltaTolerance) {
            val hiddenFallVL = (groundSpoofVerticalDeltaTolerance / yDelta) * 10.0
            illegalStates.add(
                IllegalState.GroundSpoofIllegalState(
                    hiddenFallVL,
                    "HiddenVelocity",
                ),
            )
        }

        return illegalStates
    }

    private var detectedIllegalStates: List<IllegalState>? = null

    override fun onTick() {
        val player = player ?: return
        if (player.isOnGround) {
            airTicks = 0
        } else {
            airTicks++
        }
        detectedIllegalStates = detect(player)
    }

    override fun render2d(graphics2D: Graphics2D) {
        if (!debugHudSetting.value) return
        val detectedIllegalStates = detectedIllegalStates ?: return
        if (detectedIllegalStates.isEmpty()) return
        val illegalTexts =
            detectedIllegalStates.map {
                "[AntiVulcan]:${it.name} - ${it.description} (VL:${"%.1f".format(it.violationLevel)})"
            }
        illegalTexts.forEachIndexed { i, text ->
            graphics2D.drawText(text, 0, i * graphics2D.fontHeight(), InfiniteClient.currentColors().foregroundColor)
        }
    }
}
