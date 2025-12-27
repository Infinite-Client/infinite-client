package org.infinite.features.rendering.camera

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.feature.ConfigurableFeature
import org.infinite.libs.graphics.Graphics3D
import org.infinite.settings.FeatureSetting
import org.infinite.settings.Property
import org.lwjgl.glfw.GLFW
import kotlin.math.cos
import kotlin.math.sin

class FreeCamera : ConfigurableFeature(initialEnabled = false) {
    override val level: FeatureLevel = FeatureLevel.Extend
    override val toggleKeyBind: Property<Int> = Property(GLFW.GLFW_KEY_U)
    private val speed: FeatureSetting.FloatSetting =
        FeatureSetting.FloatSetting(
            "Speed",
            1.0f,
            0.1f,
            5.0f,
        )
    override val settings: List<FeatureSetting<*>> =
        listOf(
            speed,
        )
    private var currentMode: GameType? = GameType.SURVIVAL

    override fun onDisabled() {
        val player = player ?: return
        player.setPosRaw(originalPos.x, originalPos.y, originalPos.z)
        player.yRot = originalYaw
        player.xRot = originalPitch
    }

    override fun onStart() {
        disable()
    }

    private var originalPos = Vec3.ZERO
    private var originalYaw: Float = 0.0f
    private var originalPitch: Float = 0.0f
    private var originalIsOnGround: Boolean = false
    private var originalHorizontalCollision: Boolean = false
    private var lastHealth: Float = 0.0f // Store player's health from previous tick

    override fun onEnabled() {
        originalPos = player?.eyePosition ?: Vec3.ZERO
        originalYaw = player?.yRot ?: 0.0f
        originalPitch = player?.xRot ?: 0.0f
        originalIsOnGround = player?.onGround() ?: true
        originalHorizontalCollision = player?.horizontalCollision ?: false
        lastHealth = player?.health ?: 0.0f // Capture initial health
    }

    override fun onTick() {
        if (currentMode == null) {
            disable()
            return
        }
        val player = client.player ?: return
        val options = client.options ?: return

        // Damage detection
        val currentHealth = player.health
        if (currentHealth < lastHealth) {
            disable() // Disable FreeCamera if player takes damage
            return
        }
        lastHealth = currentHealth // Update lastHealth for the next tick

        // Send a "still" packet with original position and current rotation
        if (player.connection != null) {
            player.connection.send(
                ServerboundMovePlayerPacket.PosRot(
                    originalPos.x,
                    originalPos.y,
                    originalPos.z,
                    player.yRot, // Use current player yaw
                    player.xRot, // Use current player pitch
                    originalIsOnGround,
                    originalHorizontalCollision,
                ),
            )
        }

        player.deltaMovement = Vec3.ZERO
        player.abilities.flying = false
        player.setOnGround(false)
        val moveForward = options.keyUp.isDown
        val moveBackward = options.keyDown.isDown
        val moveLeft = options.keyLeft.isDown
        val moveRight = options.keyRight.isDown
        val moveTop = options.keyJump.isDown
        val moveBottom = options.keyShift.isDown
        // 水平方向の速度は0
        // 現在のY軸の回転角度 (Yaw) をラジアンに変換
        val yawRadians = Math.toRadians(player.yRot.toDouble())
        var deltaX = 0.0
        var deltaY = 0.0
        var deltaZ = 0.0
        if (moveForward) deltaZ += 1.0
        if (moveBackward) deltaZ -= 1.0
        if (moveLeft) deltaX += 1.0 // Minecraftの左移動は、Z軸に対する回転として計算される
        if (moveRight) deltaX -= 1.0
        if (moveTop) deltaY += 1.0
        if (moveBottom) deltaY -= 1.0
        // 移動ベクトルを正規化 (斜め移動時に速くなりすぎないように)
        val magnitude = kotlin.math.sqrt(deltaX * deltaX + deltaZ * deltaZ + deltaY * deltaY)
        if (magnitude > 0) {
            deltaX /= magnitude
            deltaY /= magnitude
            deltaZ /= magnitude
        }

        // プレイヤーの視線方向に合わせて移動ベクトルを回転
        // cos(yaw) と sin(yaw) は方向ベクトルをワールド座標に変換するために使用される
        val velocityX = deltaX * cos(yawRadians) - deltaZ * sin(yawRadians)
        val velocityZ = deltaZ * cos(yawRadians) + deltaX * sin(yawRadians)
        // 速度設定を適用してプレイヤーに速度を設定
        val currentSpeed = speed.value.toDouble()
        player.deltaMovement =
            Vec3(
                velocityX * currentSpeed,
                deltaY * currentSpeed,
                velocityZ * currentSpeed,
            )
    }

    override fun render3d(graphics3D: Graphics3D) {
        val lineColor = InfiniteClient.theme().colors.infoColor
        val currentPos = client.player?.getPosition(graphics3D.tickCounter.getGameTimeDeltaPartialTick(true)) ?: return
        graphics3D.renderLine(originalPos, currentPos, lineColor, true)
    }
}
