package org.infinite.infinite.features.movement.fly

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.phys.Vec3
import org.infinite.libs.client.aim.camera.CameraRoll
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting
import org.infinite.utils.toRadians
import kotlin.math.cos
import kotlin.math.sin

enum class FlyMethod {
    Acceleration,
    Rocket,
    CreativeFlight,
}

class SuperFly : ConfigurableFeature(initialEnabled = false) {
    override val level: FeatureLevel = FeatureLevel.Cheat
    private val method: FeatureSetting.EnumSetting<FlyMethod> =
        FeatureSetting.EnumSetting(
            "Method",
            FlyMethod.Acceleration,
            FlyMethod.entries,
        )
    private val keepFly: FeatureSetting.BooleanSetting =
        FeatureSetting.BooleanSetting(
            "KeepFlying",
            true,
        )
    private val power: FeatureSetting.FloatSetting =
        FeatureSetting.FloatSetting("Power", 1.0f, 0.5f, 5.0f)
    override val settings: List<FeatureSetting<*>> =
        listOf(
            method,
            power,
            keepFly,
        )

    override fun onTick() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        if (!player.isFallFlying && method.value != FlyMethod.CreativeFlight) return
        manageGliding(client)
        when (method.value) {
            FlyMethod.Acceleration -> {
                // Check HyperBoost conditions: HyperBoost enabled, forward key, jump key, and sneak key pressed
                val isHyperBoostActive =
                    client.options.keyUp.isDown &&
                        client.options.keyJump.isDown &&
                        client.options.keyShift.isDown

                if (isHyperBoostActive) {
                    // Apply HyperBoost effects
                    applyAccelerationHyperBoost(client)
                } else {
                    // Apply normal speed and height controls
                    controlAccelerationSpeed(client)
                    controlAccelerationHeight(client)
                }
            }

            FlyMethod.Rocket -> {
                // ロケットモードでは、急停止や全方向への移動を可能にするため、キーのチェックをcontrolRocket内部で行います。
                controlRocket(client)
            }

            FlyMethod.CreativeFlight -> {
                controlCreativeFlight(client)
            }
        }
    }

    private fun manageGliding(client: Minecraft) {
        val player = client.player ?: return
        val options = client.options ?: return
        // Only apply this specific gliding management if we are in a method that relies on elytra gliding
        if (method.value == FlyMethod.Acceleration || method.value == FlyMethod.Rocket) {
            if (player.isInWater) {
                val packet =
                    ServerboundPlayerCommandPacket(
                        player,
                        ServerboundPlayerCommandPacket.Action.START_FALL_FLYING,
                    )
                player.connection?.send(packet)
            }
        }
        val cancelKey = options.keySprint.isDown && options.keyShift.isDown
        if (keepFly.value && !player.isFallFlying && !cancelKey) {
            val packet =
                ServerboundPlayerCommandPacket(
                    player,
                    ServerboundPlayerCommandPacket.Action.START_FALL_FLYING,
                )
            player.connection?.send(packet)
        }
    }

    private fun controlAccelerationSpeed(client: Minecraft) {
        val player = client.player ?: return
        val yaw = toRadians(player.yRot)
        val velocity = player.deltaMovement
        val movementPower = 0.05 + power.value / 100.0
        val forwardVelocity =
            Vec3(
                -sin(yaw) * movementPower,
                0.0,
                cos(yaw) * movementPower,
            )
        if (client.options.keyUp.isDown) {
            player.setDeltaMovement(velocity.add(forwardVelocity))
        }
        if (client.options.keyDown.isDown) {
            player.setDeltaMovement(velocity.subtract(forwardVelocity))
        }
    }

    private fun controlAccelerationHeight(client: Minecraft) {
        val player = client.player ?: return
        val velocity = player.deltaMovement
        val movementPower = 0.06 + power.value / 100
        val gravity = 0.02
        if (client.options.keyJump.isDown) {
            player.setDeltaMovement(velocity.x, velocity.y + movementPower + gravity, velocity.z)
        }
        if (client.options.keyShift.isDown) {
            player.setDeltaMovement(velocity.x, velocity.y - movementPower + gravity, velocity.z)
        }
    }

    private fun applyAccelerationHyperBoost(client: Minecraft) {
        val player = client.player ?: return
        val yaw = toRadians(player.yRot)
        val velocity = player.deltaMovement
        val movementPower = 0.3 + power.value / 100.0
        // HyperBoost: Significantly increase forward speed and add slight upward boost
        val hyperBoostVelocity =
            Vec3(
                -sin(yaw) * movementPower, // Increased speed (0.05 -> 0.3)
                0.1, // Slight upward boost
                cos(yaw) * movementPower, // Increased speed (0.05 -> 0.3)
            )
        player.setDeltaMovement(velocity.add(hyperBoostVelocity))
    }

    /**
     * Rocketモードの操作を制御します。
     * - 前/後キー: 視線方向に沿って移動
     * - 左/右キー: 水平にストレイフ移動
     * - ジャンプキー: 真上へ移動 (ワールドY+)
     * - スニークキー (Shift): 即座に停止 (急停止)
     */
    private fun controlRocket(client: Minecraft) {
        val player = client.player ?: return
        val options = client.options ?: return
        // power.valueに応じて速度を設定します。2.0を乗算してデフォルトの速度を調整します。
        val movementMultiplier = power.value * 2.0

        // SHIFTキー (Sneak Key) が押されている場合、速度をゼロにして即座に停止します。
        if (options.keyShift.isDown) {
            player.setDeltaMovement(Vec3.ZERO)
            return
        }

        val yaw = toRadians(player.yRot)

        var moveVector = Vec3.ZERO
        var moving = false

        // 前後移動 (W/S) - 視線方向
        if (options.keyUp.isDown || options.keyDown.isDown) {
            // CameraRollを使用して、ピッチ（上下方向）も考慮した視線方向のベクトルを取得
            val forwardDirection = CameraRoll(player.yRot.toDouble(), player.xRot.toDouble()).vec()
            if (options.keyUp.isDown) {
                moveVector = moveVector.add(forwardDirection)
            }
            if (options.keyDown.isDown) {
                moveVector = moveVector.subtract(forwardDirection)
            }
            moving = true
        }

        // 左右移動 (A/D) - 水平方向のストレイフ
        if (options.keyLeft.isDown || options.keyRight.isDown) {
            // 水平方向の左右移動ベクトルを計算 (視線方向のYawに90度回転)
            // rightX = cos(yaw), rightZ = sin(yaw)
            val strafeX = cos(yaw).toDouble()
            val strafeZ = sin(yaw).toDouble()
            val strafeVec = Vec3(strafeX, 0.0, strafeZ)

            if (options.keyRight.isDown) {
                moveVector = moveVector.subtract(strafeVec)
            }
            if (options.keyLeft.isDown) {
                moveVector = moveVector.add(strafeVec)
            }
            moving = true
        }

        // 上移動 (Jump Key) - 真上 (ワールドY軸)
        if (options.keyJump.isDown) {
            moveVector = moveVector.add(Vec3(0.0, 1.0, 0.0))
            moving = true
        }

        // 移動キーが押されている場合のみ速度を更新
        if (moving) {
            // 速度ベクトルを正規化し、設定されたパワーを適用
            // 正規化することで、斜めや複数キー同時押しの場合でも一定の速度を保ちます
            val finalVelocity = moveVector.normalize().scale(movementMultiplier)
            player.setDeltaMovement(finalVelocity)
        } else {
            // 移動キーが何も押されていない場合 (スニークキーは上記で処理済み)、
            // 新しい速度を設定しないことで、ゲームの物理演算（重力、空気抵抗）に速度の減衰を任せます。
        }
    }

    private fun controlCreativeFlight(client: Minecraft) {
        val player = client.player ?: return
        if (!player.isFallFlying) return
        val options = client.options ?: return
        val baseSpeed = power.value
        val boostMultiplier = if (player.isSprinting) 2.0 else 1.0 // スプリント（Ctrl）で速度ブースト
        val gravity = 0.02
        var deltaX = 0.0
        var deltaY = 0.0
        var deltaZ = 0.0

        // 2. 移動キーのチェック
        if (options.keyUp.isDown) deltaZ += 1.0
        if (options.keyDown.isDown) deltaZ -= 1.0
        if (options.keyLeft.isDown) deltaX += 1.0
        if (options.keyRight.isDown) deltaX -= 1.0

        // 上下移動 (Jump Key for Up, Sneak Key for Down)
        if (options.keyJump.isDown) deltaY += 1.0
        if (options.keyShift.isDown) deltaY -= 1.0

        // 移動ベクトルを正規化 (斜め移動時に速くなりすぎないように)
        val magnitude = kotlin.math.sqrt(deltaX * deltaX + deltaZ * deltaZ + deltaY * deltaY)
        if (magnitude > 0) {
            deltaX /= magnitude
            deltaY /= magnitude
            deltaZ /= magnitude
        }

        // 3. プレイヤーの視線方向に合わせて水平移動ベクトルを回転
        // Yaw (Y軸回転) をラジアンに変換
        val yawRadians = toRadians(player.yRot)

        // Yawに基づいて水平方向の速度をワールド座標に変換 (FreeCameraのロジックと同様)
        val velocityX = deltaX * cos(yawRadians) - deltaZ * sin(yawRadians)
        val velocityZ = deltaZ * cos(yawRadians) + deltaX * sin(yawRadians)

        // 4. 速度を適用
        val currentSpeed = baseSpeed * boostMultiplier
        // クリエイティブ飛行は、速度を設定するだけでなく、プレイヤーの慣性（既存の速度）を徐々に減衰させる特性があります。
        // 完全なバニラ動作をエミュレートするには、既存の速度を考慮しつつ新しい速度を加える必要があります。
        // ここでは単純に速度を設定することで、常に一定の速度で移動できるようにします。
        player.setDeltaMovement(
            Vec3(
                velocityX * currentSpeed,
                deltaY * currentSpeed + gravity,
                velocityZ * currentSpeed,
            ),
        )
    }
}
