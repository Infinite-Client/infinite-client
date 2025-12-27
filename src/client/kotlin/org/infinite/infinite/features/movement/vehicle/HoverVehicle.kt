package org.infinite.infinite.features.movement.vehicle

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.vehicle.VehicleEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.feature.ConfigurableFeature
import org.infinite.infinite.features.automatic.pilot.AutoPilot
import org.infinite.libs.client.aim.camera.CameraRoll
import org.infinite.settings.FeatureSetting
import org.infinite.utils.toRadians
import kotlin.math.cos
import kotlin.math.sin

class HoverVehicle : ConfigurableFeature(initialEnabled = false) {
    override val level = FeatureLevel.Cheat

    // 新しい設定: 最高速度 (m/s)
    private val speed: FeatureSetting.FloatSetting =
        FeatureSetting.FloatSetting("MaxSpeed", 20.0f, 1.0f, 200.0f)

    // 新しい設定: 加速力 (毎ティック)
    private val acceleration: FeatureSetting.FloatSetting =
        FeatureSetting.FloatSetting(
            "Acceleration",
            0.5f,
            0.05f,
            2.0f,
        )

    override val settings: List<FeatureSetting<*>> = listOf(speed, acceleration)

    override fun onTick() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val vehicle = player.vehicle
        // --- 自動再搭乗ロジック (変更なし) ---
        if (vehicle == null) {
            val world = client.level ?: return
            val x = player.x
            val y = player.y
            val z = player.z
            val reach = player.entityInteractionRange()
            val nearbyVehicle =
                world
                    .getEntities(
                        player,
                        AABB(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach),
                    ) { entity -> entity is VehicleEntity } // 全ての乗り物エンティティ
                    .minByOrNull { it.distanceToSqr(player) }
            if (nearbyVehicle != null) {
                player.interactOn(nearbyVehicle, InteractionHand.MAIN_HAND)
                return
            }

            return
        }

        // ボートに乗っている間の操作を制御
        controlBoatMovement(client, vehicle)
    }

    /**
     * ボートに乗っている間の移動操作を制御します。
     * 加速力と最高速度の設定を使用して、ボートの速度を制御します。
     */
    private fun controlBoatMovement(
        client: Minecraft,
        vehicle: Entity,
    ) {
        val options = client.options ?: return
        val player = client.player ?: return
        val maxSpeed = speed.value.toDouble() / 20 // 設定された最高速度 (m/s)
        val accelPerTick = acceleration.value.toDouble()
        var inputVector = Vec3.ZERO
        var moving = false
        val yaw = player.yRot
        val pitch = player.xRot
        val yawRadians = toRadians(yaw)
        vehicle.isNoGravity = true
        // --- 望ましい移動方向ベクトルを計算 ---

        // 前後移動 (W/S) - 視線方向
        if (options.keyUp.isDown || options.keyDown.isDown) {
            val forwardDirection = CameraRoll(yaw.toDouble(), pitch.toDouble()).vec().normalize()
            if (options.keyUp.isDown) {
                inputVector = inputVector.add(forwardDirection)
            }
            if (options.keyDown.isDown) {
                inputVector = inputVector.subtract(forwardDirection)
            }
            moving = true
        }

        // 左右移動 (A/D) - 水平方向のストレイフ
        if (options.keyLeft.isDown || options.keyRight.isDown) {
            val strafeX = cos(yawRadians).toDouble()
            val strafeZ = sin(yawRadians).toDouble()
            // ストレイフベクトル (水平)
            val strafeVec = Vec3(strafeX, 0.0, strafeZ)

            if (options.keyRight.isDown) {
                inputVector = inputVector.subtract(strafeVec) // 右移動
            }
            if (options.keyLeft.isDown) {
                inputVector = inputVector.add(strafeVec) // 左移動
            }
            moving = true
        }

        // 上昇/下降移動
        // 垂直方向の移動 (Jump/Sneak)
        if (options.keyJump.isDown) {
            inputVector = inputVector.scale(acceleration.value + 1.0)
            moving = true
        }
        // 注意: 原子のコードの 'options.sneakKey.isPressed = options.sneakKey.isPressed && player.isOnGround' は
        // クライアントオプションを書き換える不適切な処理のため削除または無効化を推奨。
        // ここでは一旦削除。

        // --- 速度制御ロジック ---

        val currentVelocity = vehicle.deltaMovement

        if (moving) {
            // 望ましい移動方向を正規化
            // 現在の速度と望ましい速度の差を計算し、加速を適用
            // 最高速度を適用した望ましい速度ベクトル
            val desiredVelocity =
                if (inputVector.length() > maxSpeed) {
                    val desiredDirection = inputVector.normalize()
                    desiredDirection.scale(maxSpeed)
                } else {
                    inputVector
                }
            // 加速によって変化する速度ベクトル
            var accelerationVector = desiredVelocity.subtract(currentVelocity)

            // 加速ベクトルが大きすぎる場合、`accelPerTick` に制限
            if (accelerationVector.lengthSqr() > accelPerTick * accelPerTick) {
                accelerationVector = accelerationVector.normalize().scale(accelPerTick)
            }

            // 新しい速度を計算
            var newVelocity = currentVelocity.add(accelerationVector)

            // 最高速度でクランプ (丸め誤差などによる超過防止)
            if (newVelocity.lengthSqr() > maxSpeed * maxSpeed) {
                newVelocity = newVelocity.normalize().scale(maxSpeed)
            }

            vehicle.deltaMovement = newVelocity
        } else {
            // 移動キーが押されていない場合、減速（摩擦）を適用して停止させる
            // AutoPilotとの整合性を確保
            // 摩擦係数を設定 (例: 10% 減速)
            val friction = 0.90 // 10%減速

            // 加速力よりも速く停止させるために、加速力に基づいた減速を適用
            val currentSpeed = currentVelocity.length()
            if (currentSpeed > 0.0) {
                val stopDistance = currentSpeed * (1.0 - friction) // このティックで減速する量

                if (stopDistance > currentSpeed) {
                    // 減速量が大きすぎて速度を反転させる場合、完全に停止させる
                    vehicle.deltaMovement = Vec3.ZERO
                } else {
                    // 速度に摩擦係数を適用
                    if (InfiniteClient.isSettingEnabled(AutoPilot::class.java, "JeetFlight")) {
                        vehicle.deltaMovement = currentVelocity.multiply(1.0, friction, 1.0)
                    } else {
                        vehicle.deltaMovement = currentVelocity.scale(friction)
                    }
                }
            } else {
                // 完全に停止している場合
                vehicle.deltaMovement = Vec3.ZERO
            }
        }
    }

    /**
     * Featureが無効になったときに、ボートの重力設定をリセットします。
     * ボートが物理演算の影響を受けるように、ボートの重力を有効にします。
     */
    override fun onDisabled() {
        val client = Minecraft.getInstance()
        val player = client.player
        val vehicle = player?.vehicle
        vehicle?.isNoGravity = false
    }
}
