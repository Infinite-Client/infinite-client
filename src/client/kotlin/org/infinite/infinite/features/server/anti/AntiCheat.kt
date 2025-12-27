package org.infinite.infinite.features.server.anti

import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.feature.ConfigurableFeature
import org.infinite.infinite.features.movement.move.QuickMove
import org.infinite.settings.FeatureSetting

class AntiCheat : ConfigurableFeature(initialEnabled = true) {
    override val tickTiming: Timing = Timing.End
    private val enableForQuickMove = FeatureSetting.BooleanSetting("EnableForQuickMove", false)
    private val quickMoveTolerance = FeatureSetting.DoubleSetting("QuickMoveTolerance", 0.05, 0.0, 0.3)
    private val quickMoveLatencyMultiplier = FeatureSetting.DoubleSetting("QuickMoveLatencyMultiplier", 0.5, 0.0, 1.0)
    override val settings: List<FeatureSetting<*>> =
        listOf(enableForQuickMove, quickMoveTolerance, quickMoveLatencyMultiplier)

    override fun onTick() {
        if (enableForQuickMove.value) {
            handleQuickMove()
        }
    }

    private fun handleQuickMove() {
        val quickMove = InfiniteClient.getFeature(QuickMove::class.java) ?: return
        val player = player ?: return
        if (player.isSprinting && player.onGround()) return
        val networkHandler = player.connection ?: return
        val netWorkPlayer = networkHandler.getPlayerInfo(player.uuid) ?: return
        val latencyMS = netWorkPlayer.latency
        val baseTolerance = quickMoveTolerance.value // 基本的な許容値 (例: 0.05)
        val latencyMultiplier = quickMoveLatencyMultiplier.value // レイテンシに適用する倍率 (例: 0.5)
        val dynamicToleranceMultiplier: Double =
            baseTolerance + (latencyMS.toDouble() * 0.001 * latencyMultiplier)
        val modifiedVelocity = quickMove.calculateVelocity()
        val originalVelocity = player.deltaMovement
        val diffVelocity = modifiedVelocity.subtract(originalVelocity) // チートによる理想値との差
        val originalSpeed = originalVelocity.horizontalDistance()
        val diffSpeed = diffVelocity.horizontalDistance()
        val diffProgress = diffSpeed / originalSpeed
        player.setDeltaMovement(
            if (diffProgress > dynamicToleranceMultiplier) {
                val m = (diffProgress / dynamicToleranceMultiplier).coerceIn(0.0, 1.0)
                val modifiedVelocity =
                    vecMin(
                        modifiedVelocity,
                        originalVelocity.add(diffVelocity.scale(m)),
                    )
                modifiedVelocity
            } else {
                modifiedVelocity
            },
        )
    }

    private fun vecMin(
        a: Vec3,
        b: Vec3,
    ): Vec3 =
        if (a.lengthSqr() > b.lengthSqr()) {
            b
        } else {
            a
        }
}
