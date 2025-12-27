package org.infinite.infinite.features.movement.sprint

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting
import kotlin.math.atan2

class SuperSprint : ConfigurableFeature(initialEnabled = false) {
    private val onlyWhenForward =
        FeatureSetting.BooleanSetting(
            "OnlyWhenForward",
            true,
        )
    private val evenIfHungry =
        FeatureSetting.BooleanSetting(
            "EvenIfHungry",
            false,
        )

    override val settings: List<FeatureSetting<*>> =
        listOf(
            onlyWhenForward,
            evenIfHungry,
        )

    override fun onDisabled() {
        options.keySprint.isDown = false
    }

    override fun onTick() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val options = client.options ?: return
        if (!evenIfHungry.value) { // If EvenIfHungry is false, check hunger
            if (player.foodData.foodLevel <= 6) {
                player.isSprinting = false // Stop sprinting if hunger is too low
                return // Prevent further sprint logic
            }
        }
        if (!options.keySprint.isDown) {
            options.keySprint.isDown = !player.isFallFlying && options.keyUp.isDown
        }
        if (!onlyWhenForward.value) {
            val pressedForward = options.keyUp.isDown
            val pressedBack = options.keyDown.isDown
            val pressedLeft = options.keyLeft.isDown
            val pressedRight = options.keyRight.isDown
            val movementKeyPressed = pressedForward || pressedBack || pressedLeft || pressedRight
            if (movementKeyPressed) {
                player.isSprinting = true
                val currentYaw = player.yRot // 現在のカメラの向き（視線）
                var deltaYaw: Double // ラジアン
                val moveZ = (if (pressedForward) 1 else 0) - (if (pressedBack) 1 else 0)
                val moveX = (if (pressedRight) 1 else 0) - (if (pressedLeft) 1 else 0)
                if (moveZ != 0 || moveX != 0) {
                    deltaYaw = atan2(moveX.toDouble(), moveZ.toDouble())
                    val calculatedYaw = (currentYaw + Math.toDegrees(deltaYaw)).toFloat()
                    val networkHandler = client.connection
                    if (networkHandler != null) {
                        // PlayerMoveC2SPacket.LookAndOnGround で向きをサーバーに強制
                        val packet =
                            ServerboundMovePlayerPacket.Rot(
                                calculatedYaw,
                                player.xRot, // Pitchは変更しない
                                player.onGround(),
                                player.horizontalCollision,
                            )
                        networkHandler.send(packet)
                    }
                }
            }
        }
    }
}
