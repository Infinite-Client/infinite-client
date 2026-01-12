package org.infinite.infinite.features.local.combat.mace

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.item.Items
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MaceBoostFeature : LocalFeature() {
    override val featureType = FeatureType.Cheat

    enum class BoostMode {
        InstantKill,
        Adaptive,
        Safe,
    }

    val mode by property(EnumSelectionProperty(BoostMode.Adaptive))
    val targetFallDist by property(DoubleProperty(500.0, 1.0, 1000.0))
    val fallMultiply by property(DoubleProperty(1.5, 1.0, 5.0))
    val minCharge by property(DoubleProperty(0.9, 0.0, 1.0))

    // 1パケットあたりの最大移動距離（サーバーの検知回避用）
    val maxStep by property(DoubleProperty(8.0, 0.1, 20.0))

    fun onPreAttack() {
        val player = minecraft.player ?: return
        if (player.mainHandItem.item != Items.MACE) return
        if (player.getAttackStrengthScale(0f) < minCharge.value) return

        applyBoost()
    }

    private fun applyBoost() {
        val player = minecraft.player ?: return
        val currentFall = player.fallDistance
        val minDistance = 1.5

        val targetDist = when (mode.value) {
            BoostMode.Safe -> minDistance
            BoostMode.Adaptive -> max(minDistance, currentFall * fallMultiply.value)
            BoostMode.InstantKill -> maxOf(currentFall * fallMultiply.value, targetFallDist.value, minDistance)
        }

        if (currentFall >= targetDist) return

        // 1. バッファ埋め
        repeat(4) {
            sendFakePos(0.0)
        }

        // 2. ステップ分割送信
        // 目標とする高さの増分（sqrt(targetDist)）を maxStep ずつに分けて送信する
        var remainingOffset = sqrt(targetDist)
        val stepSize = maxStep.value

        while (remainingOffset > 0) {
            val currentStep = min(remainingOffset, stepSize)
            sendFakePos(currentStep)
            remainingOffset -= currentStep
        }
        sendFakePos(0.0)
    }
    private fun sendFakePos(offset: Double) {
        val p = minecraft.player ?: return
        // Maceダメージ判定には「空中(onGround=false)」である必要がある
        connection?.send(
            ServerboundMovePlayerPacket.Pos(
                p.x,
                p.y + offset,
                p.z,
                false,
                p.horizontalCollision,
            ),
        )
    }
}
