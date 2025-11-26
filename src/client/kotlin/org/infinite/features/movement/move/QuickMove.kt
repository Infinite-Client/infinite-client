package org.infinite.features.movement.move

import net.minecraft.block.Blocks
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.util.math.Vec3d
import org.infinite.InfiniteClient
import org.infinite.feature.ConfigurableFeature
import org.infinite.features.server.anti.AntiCheat
import org.infinite.settings.FeatureSetting
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

class QuickMove : ConfigurableFeature() {
    override val tickTiming: Timing = Timing.End

    // 基準となる各環境の移動速度（ブロック/秒）を定義
    private val currentMode: MoveMode
        get() {
            val player = player ?: return MoveMode.None
            return when {
                player.hasVehicle() && allowWithVehicle.value -> MoveMode.Vehicle
                allowOnSwimming.value && player.isSwimming -> MoveMode.Swimming
                allowOnGliding.value && player.isGliding -> MoveMode.Gliding
                player.isOnGround && allowOnGround.value -> MoveMode.Ground
                player.isInLava && allowInLava.value -> MoveMode.Lava
                player.isTouchingWater && allowInWater.value -> MoveMode.Water
                !player.isOnGround && allowInAir.value -> MoveMode.Air
                else -> MoveMode.None
            }
        }
    private val reductionThreshold = FeatureSetting.DoubleSetting("ReductionThreshold", 10.0, 0.0, 100.0)
    private val itemUseBoost = FeatureSetting.DoubleSetting("BoostWhenUseItem", 0.5, 0.0, 1.0)
    private val currentAcceleration: Double
        get() {
            val player = player ?: return 0.0
            val attributes = player.attributes
            return when (currentMode) {
                MoveMode.Vehicle -> {
                    val vehicle = player.vehicle ?: return 0.0
                    when (vehicle) {
                        is LivingEntity -> {
                            vehicle.attributes.getValue(EntityAttributes.MOVEMENT_SPEED)
                        }

                        is BoatEntity -> {
                            1.0
                        }

                        else -> {
                            0.0
                        }
                    }
                }

                else -> {
                    (if (player.isSprinting) 1.3 else 1.0) *
                        attributes.getValue(
                            EntityAttributes.MOVEMENT_SPEED,
                        )
                }
            }
        }
    private val currentFriction: Double
        get() {
            val player = player ?: return 0.0
            val world = world ?: return 0.0
            val entity = player.vehicle ?: player
            val attributes = player.attributes
            val blockPos = entity.supportingBlockPos

            val blockFriction =
                if (blockPos != null && blockPos.isPresent) {
                    world
                        .getBlockState(
                            blockPos.get(),
                        ).block.slipperiness
                } else {
                    1f
                }
            val poseFriction = if (player.isSneaking) attributes.getValue(EntityAttributes.SNEAKING_SPEED) else 1.0
            val airFriction = 0.91
            val waterFriction = Blocks.WATER.slipperiness
            val lavaFriction = Blocks.LAVA.slipperiness
            return when (currentMode) {
                MoveMode.Ground -> {
                    return blockFriction * poseFriction * airFriction
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

    private val accelerationConstant =
        FeatureSetting.DoubleSetting(
            "AccelerationConstant",
            0.02,
            0.0,
            1.0,
        )
    private val accelerationMultiplier =
        FeatureSetting.DoubleSetting(
            "AccelerationMultiplier",
            1.1,
            1.0,
            2.0,
        )
    private val friction = FeatureSetting.DoubleSetting("Friction", 1.0, 0.0, 1.0)

    // --- 速度設定値 ---
    private val speed =
        FeatureSetting.DoubleSetting(
            "Speed",
            0.75,
            0.0,
            2.0,
        )
    private val antiFrictionBoost = FeatureSetting.DoubleSetting("AntiFrictionBoost", 1.0, 0.0, 5.0)
    private val antiFrictionPoint = FeatureSetting.DoubleSetting("AntiFrictionPoint", 0.75, 0.0, 1.0)

    // --- Allow設定値 ---
    private val allowOnGround = FeatureSetting.BooleanSetting("AllowOnGround", true)
    private val allowInWater = FeatureSetting.BooleanSetting("AllowInWater", false)
    private val allowInLava = FeatureSetting.BooleanSetting("AllowInLava", false)
    private val allowWithVehicle =
        FeatureSetting.BooleanSetting(
            "AllowWithVehicle",
            false,
        )

    private val allowInAir =
        FeatureSetting.BooleanSetting(
            "AllowInAir",
            false,
        )
    private val allowOnGliding =
        FeatureSetting.BooleanSetting(
            "AllowOnGliding",
            false,
        )
    private val allowOnSwimming =
        FeatureSetting.BooleanSetting(
            "AllowOnSwimming",
            false,
        )

    override val settings: List<FeatureSetting<*>> =
        listOf(
            accelerationConstant,
            accelerationMultiplier,
            friction,
            speed,
            reductionThreshold,
            antiFrictionBoost,
            antiFrictionPoint,
            itemUseBoost,
            allowOnGround,
            allowOnSwimming,
            allowInWater,
            allowInLava,
            allowInAir,
            allowOnGliding,
            allowWithVehicle,
        )

    override fun onEnabled() {
        lastVelocity = player?.velocity ?: Vec3d.ZERO
    }

    var lastVelocity: Vec3d = Vec3d.ZERO
    var playerAccelerationSpeed: Double = 0.0

    fun updatePlayerAccelerationSpeed() {
        val player = player ?: return
        val v = player.velocity
        val l = lastVelocity
        // 水平方向の加速度を計算
        playerAccelerationSpeed = sqrt((v.x - l.x).pow(2) + (v.z - l.z).pow(2))
        lastVelocity = player.velocity
    }

    /**
     * 現在の状態と設定に基づき、プレイヤーまたは車両の新しいベロシティ（水平成分）を計算します。
     * @return 新しい水平ベロシティ成分 (Vec3d(newVelX, 0.0, newVelZ))。Y成分は無視されます。
     */
    fun calculateVelocity(): Vec3d {
        val player = player ?: return Vec3d.ZERO
        val options = options
        val velocity = velocity ?: return Vec3d.ZERO // 現在のベロシティ

        var forwardInput = 0.0
        var strafeInput = 0.0

        if (options.forwardKey.isPressed) forwardInput++
        if (options.backKey.isPressed) forwardInput--
        if (options.leftKey.isPressed) strafeInput++
        if (options.rightKey.isPressed) strafeInput--

        val tickSpeedLimit = currentMaxSpeed * speed.value
        val baseAcceleration = accelerationConstant.value // 設定された基本の加速度

        // 1. グローバル速度をプレイヤーのローカル座標系に変換
        val yaw = Math.toRadians(player.yaw.toDouble())
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
                localVelForward *= friction.value
            }
        }

        // Strafe (左右方向) の減速
        if (localVelStrafe != 0.0) {
            // localVelStrafeとstrafeInputの符号が異なる場合
            if (sign(localVelStrafe) != sign(strafeInput)) {
                localVelStrafe *= friction.value
            }
        }

        // 3. 速度制限と加速の計算
        val currentMoveSpeed = sqrt(localVelForward * localVelForward + localVelStrafe * localVelStrafe)
        val delta = reductionThreshold.value / 100.0
        val currentFriction = currentFriction // 環境摩擦
        val antiFrictionBoost = antiFrictionBoost.value
        val antiFrictionPoint = antiFrictionPoint.value

        // 環境摩擦(currentFriction)が設定値(antiFrictionPoint)より低い場合に、加速ブーストを適用
        val antiFrictionFactor = (
            1 + (antiFrictionPoint - currentFriction) *
                (1.0 / antiFrictionPoint).coerceIn(
                    0.0,
                    1.0,
                ) * antiFrictionBoost
        )

        val isApplyingCorrection = player.isUsingItem && player.isOnGround
        val itemUseFactor =
            if (isApplyingCorrection) {
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

        val accelerationFactor: Double =
            when {
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
        val currentAcceleration =
            (
                baseAcceleration * antiFrictionFactor *
                    accelerationFactor.coerceIn(
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
        return Vec3d(newVelX, 0.0, newVelZ)
    }

    override fun onTick() {
        // 加速度更新をTickの最初に行う
        if (InfiniteClient.isFeatureEnabled(AntiCheat::class.java)) return
        updatePlayerAccelerationSpeed()
        if (currentMode == MoveMode.None) return
        val player = player ?: return
        val vehicle = player.vehicle

        // 車両が有効な場合は、プレイヤーのYawを車両に適用
        vehicle?.yaw = player.yaw

        // calculateVelocityを呼び出し、新しい水平ベロシティを取得
        val newVelocityXZ = calculateVelocity()

        // 既存のY成分を保持し、XとZ成分を更新
        this.velocity = Vec3d(newVelocityXZ.x, this.velocity?.y ?: player.velocity.y, newVelocityXZ.z)
    }
}
