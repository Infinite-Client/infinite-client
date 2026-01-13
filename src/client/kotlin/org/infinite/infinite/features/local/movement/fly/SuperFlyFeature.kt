package org.infinite.infinite.features.local.movement.fly

import net.minecraft.world.phys.Vec3
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.graphics.graphics3d.structs.CameraRoll
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

class SuperFlyFeature : LocalFeature() {
    enum class FlyMethod {
        Acceleration, Rocket, CreativeFlight,
    }

    override val featureType: FeatureType = FeatureType.Cheat
    override val categoryClass = org.infinite.infinite.features.local.movement.LocalMovementCategory::class

    private val method by property(
        EnumSelectionProperty(
            FlyMethod.Acceleration,
        ),
    )
    private val keepFly by property(
        BooleanProperty(
            true,
        ),
    )
    private val power by property(
        FloatProperty(1.0f, 0.5f, 5.0f, "Power"),
    )

    override fun onEndTick() {
        val player = player ?: return

        if (!player.isFallFlying && method.value != FlyMethod.CreativeFlight) return
        manageGliding()
        when (method.value) {
            FlyMethod.Acceleration -> {
                // Check HyperBoost conditions: HyperBoost enabled, forward key, jump key, and sneak key pressed
                val isHyperBoostActive =
                    options.keyUp.isDown && options.keyJump.isDown && options.keyShift.isDown

                if (isHyperBoostActive) {
                    // Apply HyperBoost effects
                    applyAccelerationHyperBoost()
                } else {
                    // Apply normal speed and height controls
                    controlAccelerationSpeed()
                    controlAccelerationHeight()
                }
            }

            FlyMethod.Rocket -> {
                // ロケットモードでは、急停止や全方向への移動を可能にするため、キーのチェックをcontrolRocket内部で行います。
                controlRocket()
            }

            FlyMethod.CreativeFlight -> {
                controlCreativeFlight()
            }
        }
    }

    private fun manageGliding() {
        val player = player ?: return
        val options = options

        // Only apply this specific gliding management if we are in a method that relies on elytra gliding
        if (method.value == FlyMethod.Acceleration || method.value == FlyMethod.Rocket) {
            if (player.isInWater) {
                player.stopFallFlying()
            }
        }
        val cancelKey = options.keySprint.isDown && options.keyShift.isDown
        if (keepFly.value && !player.isFallFlying && !cancelKey) {
            player.startFallFlying()
        }
    }

    private fun controlAccelerationSpeed() {
        val player = player ?: return
        val yaw = toRadians(player.yRot.toDouble())
        val velocity = player.deltaMovement
        val movementPower = 0.05 + power.value / 100.0
        val forwardVelocity = Vec3(
            -sin(yaw) * movementPower,
            0.0,
            cos(yaw) * movementPower,
        )
        if (options.keyUp.isDown) {
            player.deltaMovement = velocity.add(forwardVelocity)
        }
        if (options.keyDown.isDown) {
            player.deltaMovement = velocity.subtract(forwardVelocity)
        }
    }

    private fun controlAccelerationHeight() {
        val player = player ?: return
        val velocity = player.deltaMovement
        val movementPower = 0.06 + power.value / 100
        val gravity = 0.02
        if (options.keyJump.isDown) {
            player.deltaMovement = Vec3(velocity.x, velocity.y + movementPower + gravity, velocity.z)
        }
        if (options.keyShift.isDown) {
            player.deltaMovement = Vec3(velocity.x, velocity.y - movementPower + gravity, velocity.z)
        }
    }

    private fun applyAccelerationHyperBoost() {
        val player = player ?: return
        val yaw = toRadians(player.yRot.toDouble())
        val velocity = player.deltaMovement
        val movementPower = 0.3 + power.value / 100.0
        // HyperBoost: Significantly increase forward speed and add slight upward boost
        val hyperBoostVelocity = Vec3(
            -sin(yaw) * movementPower, // Increased speed (0.05 -> 0.3)
            0.1, // Slight upward boost
            cos(yaw) * movementPower, // Increased speed (0.05 -> 0.3)
        )
        player.deltaMovement = velocity.add(hyperBoostVelocity)
    }

    /**
     * Rocketモードの操作を制御します。
     * - 前/後キー: 視線方向に沿って移動
     * - 左/右キー: 水平にストレイフ移動
     * - ジャンプキー: 真上へ移動 (ワールドY+)
     * - スニークキー (Shift): 即座に停止 (急停止)
     */
    private fun controlRocket() {
        val player = player ?: return
        val options = options
        // power.valueに応じて速度を設定します。2.0を乗算してデフォルトの速度を調整します。
        val movementMultiplier = power.value * 2.0

        // SHIFTキー (Sneak Key) が押されている場合、速度をゼロにして即座に停止します。
        if (options.keyShift.isDown) {
            player.deltaMovement = Vec3.ZERO
            return
        }

        val yaw = toRadians(player.yRot.toDouble())

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
            val strafeX = cos(yaw)
            val strafeZ = sin(yaw)
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
            val finalVelocity = moveVector.normalize().multiply(movementMultiplier)
            player.deltaMovement = finalVelocity
        } else {
            // 移動キーが何も押されていない場合 (スニークキーは上記で処理済み)、
            // 新しい速度を設定しないことで、ゲームの物理演算（重力、空気抵抗）に速度の減衰を任せます。
        }
    }

    private fun Vec3.multiply(d: Double): Vec3 = this.multiply(d, d, d)
    private fun controlCreativeFlight() {
        val player = player ?: return
        if (!player.isFallFlying) return
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
        val playerYaw = player.yRot

        val yawRadians = toRadians(playerYaw.toDouble())
        // Yawに基づいて水平方向の速度をワールド座標に変換 (FreeCameraのロジックと同様)
        val velocityX = deltaX * cos(yawRadians) - deltaZ * sin(yawRadians)
        val velocityZ = deltaZ * cos(yawRadians) + deltaX * sin(yawRadians)

        // 4. 速度を適用
        val currentSpeed = baseSpeed * boostMultiplier
        // クリエイティブ飛行は、速度を設定するだけでなく、プレイヤーの慣性（既存の速度）を徐々に減衰させる特性があります。
        // 完全なバニラ動作をエミュレートするには、既存の速度を考慮しつつ新しい速度を加える必要があります。
        // ここでは単純に速度を設定することで、常に一定の速度で移動できるようにします。
        player.deltaMovement = Vec3(
            velocityX * currentSpeed,
            deltaY * currentSpeed + gravity,
            velocityZ * currentSpeed,
        )
    }
}
