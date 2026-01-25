package org.infinite.infinite.features.local.movement.quickmove

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.infinite.infinite.features.local.movement.LocalMovementCategory
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.DoubleProperty
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

class QuickMoveFeature : LocalFeature() {
    override val featureType: FeatureLevel = FeatureLevel.Cheat
    override val categoryClass = LocalMovementCategory::class

    // 基準となる各環境の移動速度（ブロック/秒）を定義
    private val currentMode: MoveMode
        get() {
            val player = player ?: return MoveMode.None
            return when {
                player.vehicle != null && allowWithVehicle.value -> MoveMode.Vehicle
                allowOnSwimming.value && player.isSwimming -> MoveMode.Swimming
                allowOnGliding.value && player.isFallFlying -> MoveMode.Gliding
                player.onGround() && allowOnGround.value -> MoveMode.Ground
                player.isInLava && allowInLava.value -> MoveMode.Lava
                player.isInWater && allowInWater.value -> MoveMode.Water
                !player.onGround() && allowInAir.value -> MoveMode.Air
                else -> MoveMode.None
            }
        }
    private val reductionThreshold by property(DoubleProperty(10.0, 0.0, 100.0))
    private val itemUseBoost by property(DoubleProperty(0.5, 0.0, 1.0))
    private val currentAcceleration: Double
        get() {
            val player = player ?: return 0.0
            val attributes = player.attributes
            return when (currentMode) {
                MoveMode.Vehicle -> {
                    val vehicle = player.vehicle ?: return 0.0
                    // Vehicle自体が持つ移動速度属性、あるいはカスタム値を参照する
                    if (vehicle is LivingEntity) {
                        vehicle.attributes.getValue(Attributes.MOVEMENT_SPEED)
                    } else {
                        // ボートやマインカートなどLivingEntityでない場合
                        0.1 // デフォルト値など
                    }
                }

                else -> {
                    // プレイヤー自身の移動速度（スプリント補正込み）
                    val baseSpeed = attributes.getValue(Attributes.MOVEMENT_SPEED)
                    (if (player.isSprinting) 1.3 else 1.0) * baseSpeed
                }
            }
        }
    private val currentFriction: Double
        get() {
            val player = player ?: return 0.0
            val level = level ?: return 0.0
            val entity = player.vehicle ?: player
            val attributes = player.attributes
            val blockPos = entity.blockPosBelowThatAffectsMyMovement

            val blockFriction = level.getBlockState(
                blockPos,
            ).block.friction
            val poseFriction = if (player.isCrouching) attributes.getValue(Attributes.SNEAKING_SPEED) else 1.0
            val airFriction = 0.91
            val waterFriction = Blocks.WATER.friction
            val lavaFriction = Blocks.LAVA.friction
            return when (currentMode) {
                MoveMode.Ground -> {
                    blockFriction * poseFriction * airFriction
                }

                MoveMode.Swimming, MoveMode.Water -> {
                    waterFriction.pow(2) * poseFriction
                }

                MoveMode.Lava -> {
                    lavaFriction.pow(2) * poseFriction
                }

                MoveMode.Air, MoveMode.Gliding -> {
                    airFriction * poseFriction
                }

                // 例: 空気抵抗に近い高い摩擦（低い減速）
                MoveMode.Vehicle -> {
                    blockFriction * poseFriction * airFriction
                }

                MoveMode.None -> {
                    1.0
                }
            }
        }
    private val currentMaxSpeed: Double
        get() {
            val acceleration = currentAcceleration
            val friction = currentFriction
            // 摩擦(friction)が1.0の場合、分母が0になる可能性があるためチェック
            return if (friction < 1.0) {
                acceleration / (1.0 - friction)
            } else {
                // 摩擦が1.0以上の場合は無限大に近い値を返すか、最大値を設ける
                // ここでは安全のため、大きな固定値を返す
                100.0
            }
        }

    // 移動モードを定義し、処理の優先順位と状態を明確にする
    private enum class MoveMode {
        None,
        Ground,
        Swimming,
        Water,
        Lava,
        Air,
        Gliding,
        Vehicle,
    }

    private val accelerationConstant by property(
        DoubleProperty(
            0.02,
            0.0,
            1.0,
        ),
    )
    private val accelerationMultiplier by property(
        DoubleProperty(
            1.1,
            1.0,
            2.0,
        ),
    )
    private val frictionProperty by property(DoubleProperty(1.0, 0.0, 1.0))

    // --- 速度設定値 ---
    private val speed by property(DoubleProperty(0.75, 0.0, 2.0))
    private val antiFrictionBoost by property(DoubleProperty(1.0, 0.0, 5.0))
    private val antiFrictionPoint by property(DoubleProperty(0.75, 0.0, 1.0))

    // --- Allow設定値 ---
    private val allowOnGround by property(BooleanProperty(true))
    private val allowInWater by property(BooleanProperty(false))
    private val allowInLava by property(BooleanProperty(false))
    private val allowWithVehicle by property(BooleanProperty(false))

    private val allowInAir by property(BooleanProperty(false))
    private val allowOnGliding by property(BooleanProperty(false))
    private val allowOnSwimming by property(BooleanProperty(false))
    var lastVelocity: Vec3 = Vec3.ZERO
    var playerAccelerationSpeed: Double = 0.0

    fun updatePlayerAccelerationSpeed() {
        val player = player ?: return
        val v = player.deltaMovement
        // 初回呼び出し時にlastVelocityを初期化
        if (lastVelocity == Vec3.ZERO && v != Vec3.ZERO) {
            lastVelocity = v
        }
        val l = lastVelocity
        // 水平方向の加速度を計算
        playerAccelerationSpeed = sqrt((v.x - l.x).pow(2) + (v.z - l.z).pow(2))
        lastVelocity = player.deltaMovement
    }

    /**
     * 現在の状態と設定に基づき、プレイヤーまたは車両の新しいベロシティ（水平成分）を計算します。
     * @return 新しい水平ベロシティ成分 (Vec3(newVelX, 0.0, newVelZ))。Y成分は無視されます。
     */
    fun calculateVelocity(): Vec3 {
        val player = player ?: return Vec3.ZERO
        val options = options
        val velocity = player.deltaMovement // 現在のベロシティ
        if (currentMode == MoveMode.None) return velocity
        var forwardInput = 0.0
        var strafeInput = 0.0

        if (options.keyUp.isDown) forwardInput++
        if (options.keyDown.isDown) forwardInput--
        if (options.keyLeft.isDown) strafeInput++
        if (options.keyRight.isDown) strafeInput--

        val tickSpeedLimit = currentMaxSpeed * speed.value
        val baseAcceleration = accelerationConstant.value // 設定された基本の加速度

        // 1. グローバル速度をプレイヤーのローカル座標系に変換
        val yaw = Math.toRadians(player.yRot.toDouble())
        val sinYaw = sin(yaw)
        val cosYaw = cos(yaw)

        // 現在の水平ベロシティ
        val currentVelX = velocity.x
        val currentVelZ = velocity.z

        // ローカル速度 (Forward, Strafe) への変換
        var localVelForward = -sinYaw * currentVelX + cosYaw * currentVelZ
        var localVelStrafe = cosYaw * currentVelX + sinYaw * currentVelZ

        // 2. 減速ロジックの適用 (キー入力とベロシティの符号が異なる場合に摩擦を適用)
        // Forward (前後方向) の減速
        if (localVelForward != 0.0) {
            // localVelForwardとforwardInputの符号が異なる場合
            if (sign(localVelForward) != sign(forwardInput)) {
                localVelForward *= frictionProperty.value
            }
        }

        // Strafe (左右方向) の減速
        if (localVelStrafe != 0.0) {
            // localVelStrafeとstrafeInputの符号が異なる場合
            if (sign(localVelStrafe) != sign(strafeInput)) {
                localVelStrafe *= frictionProperty.value
            }
        }

        // 3. 速度制限と加速の計算
        val currentMoveSpeed = sqrt(localVelForward * localVelForward + localVelStrafe * localVelStrafe)
        val delta = reductionThreshold.value / 100.0
        val currentFriction = this.currentFriction // 環境摩擦
        val antiFrictionBoost = antiFrictionBoost.value
        val antiFrictionPoint = antiFrictionPoint.value

        // 環境摩擦(currentFriction)が設定値(antiFrictionPoint)より低い場合に、加速ブーストを適用
        val antiFrictionFactor = (
            1 + (antiFrictionPoint - currentFriction) * (1.0 / antiFrictionPoint).coerceIn(
                0.0,
                1.0,
            ) * antiFrictionBoost
            )

        val isApplyingCorrection = player.isUsingItem && player.onGround()
        val itemUseFactor = if (isApplyingCorrection) {
            val baseMovementReductionFactor = 0.15
            // boostの値を0.0から1.0の間に制限する
            val boost = itemUseBoost.value.coerceIn(0.0, 1.0)
            // 最終速度を決定する分母を計算
            val finalSpeedDenominator = boost * (baseMovementReductionFactor - 1.0) + 1.0
            // 速度低下を打ち消すための補正係数を計算
            1.0 / finalSpeedDenominator
        } else {
            // 補正なし
            1.0
        }

        // 速度による加速度の調整
        val startSpeed = tickSpeedLimit * antiFrictionFactor * (1 - delta) // 減速開始速度
        val endSpeed = tickSpeedLimit * antiFrictionFactor // 加速0到達速度

        val accelerationFactor: Double = when {
            // 最高速度制限未満の場合はフル加速 (加速係数1.0)
            currentMoveSpeed <= startSpeed -> {
                1.0
            }

            // 減速区間: 速度が startSpeed と endSpeed の間
            currentMoveSpeed < endSpeed -> {
                // 線形補間: 速度が endSpeed に近づくにつれて加速係数が 1.0 から 0.0 に線形に減少
                val ratio = (currentMoveSpeed - startSpeed) / (endSpeed - startSpeed)
                1.0 - ratio
            }

            // 速度が endSpeed 以上になったら加速はゼロ (加速係数0.0)
            else -> {
                0.0
            }
        }

        // 加速上限 (tickSpeedLimitを超えないようにする)
        val accelerationLimit = (endSpeed - currentMoveSpeed).coerceAtLeast(0.0)

        // 最終的な加速度の計算
        val currentAcceleration = (
            baseAcceleration * antiFrictionFactor * accelerationFactor.coerceIn(
                0.0,
                1.0,
            ) * itemUseFactor
            ).coerceAtMost(
            accelerationLimit, // 速度超過を防ぐ上限
        )

        // 4. 加速の適用
        if (currentAcceleration > 0.0) {
            val inputMagnitude = sqrt(forwardInput * forwardInput + strafeInput * strafeInput).coerceAtLeast(1.0)
            val normalizedForward = forwardInput / inputMagnitude
            val normalizedStrafe = strafeInput / inputMagnitude

            // a. 基本の加速を加算
            localVelForward += normalizedForward * currentAcceleration
            localVelStrafe += normalizedStrafe * currentAcceleration

            // b. 既存の加速度に応じたブーストを加算
            val accelerationMultiplier = accelerationMultiplier.value
            localVelForward += normalizedForward * playerAccelerationSpeed * (accelerationMultiplier - 1)
            localVelStrafe += normalizedStrafe * playerAccelerationSpeed * (accelerationMultiplier - 1)
        }

        // 5. ローカル速度をグローバル座標系に戻す
        val newVelX = -sinYaw * localVelForward + cosYaw * localVelStrafe
        val newVelZ = cosYaw * localVelForward + sinYaw * localVelStrafe

        // X, Z成分のみを更新して返す
        return Vec3(newVelX, player.deltaMovement.y, newVelZ)
    }

    override fun onEndTick() {
        // 加速度更新をTickの最初に行う
        updatePlayerAccelerationSpeed()
        val player = player ?: return
        val vehicle = player.vehicle
        vehicle?.yRot = player.yRot
        val newVelocity = calculateVelocity()
        player.deltaMovement = Vec3(newVelocity.x, newVelocity.y, newVelocity.z)
    }
}
