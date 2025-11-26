package org.infinite.features.server.anti

import org.infinite.InfiniteClient
import org.infinite.feature.ConfigurableFeature
import org.infinite.features.movement.move.QuickMove
import org.infinite.settings.FeatureSetting

class AntiCheat : ConfigurableFeature(initialEnabled = true) {
    override val tickTiming: Timing = Timing.End
    private val enableForQuickMove = FeatureSetting.BooleanSetting("EnableForQuickMove", false)
    private val quickMoveTolerance = FeatureSetting.DoubleSetting("QuickMoveTolerance", 0.05, 0.0, 0.3)
    private val quickMoveLatencyMultiplier = FeatureSetting.DoubleSetting("QuickMoveLatencyMultiplier", 0.5, 0.0, 1.0)
    override val settings: List<FeatureSetting<*>> = listOf(enableForQuickMove)

    override fun onTick() {
        if (enableForQuickMove.value) {
            handleQuickMove()
        }
    }

    private fun handleQuickMove() {
        val quickMove = InfiniteClient.getFeature(QuickMove::class.java) ?: return
        val player = player ?: return
        // スプリント中は判定を無効化するべき
        if (player.isSprinting && player.isOnGround) return
        val networkHandler = player.networkHandler ?: return
        val netWorkPlayer = networkHandler.getPlayerListEntry(player.uuid) ?: return
        // 1. レイテンシ（Ping）を取得 (単位: ミリ秒)
        val latencyMS = netWorkPlayer.latency
        // 2. 許容倍率の計算に必要なパラメータを取得
        val baseTolerance = quickMoveTolerance.value // 基本的な許容値 (例: 0.05)
        val latencyMultiplier = quickMoveLatencyMultiplier.value // レイテンシに適用する倍率 (例: 0.5)
        // 3. 許容倍率 (Tolerance Multiplier) の計算
        // レイテンシを加味した動的な許容量を算出します。
        // レイテンシが大きいほど許容量も大きくなります。
        // Pingが0msの場合、許容量はbaseToleranceのみ。
        // Pingが200msの場合、baseTolerance + (200 * 0.001 * 0.5) となる。
        val dynamicToleranceMultiplier: Double =
            baseTolerance + (latencyMS.toDouble() * 0.001 * latencyMultiplier)
        // ここから既存のロジック
        quickMove.updatePlayerAccelerationSpeed()
        val modifiedVelocity = quickMove.calculateVelocity()
        val originalVelocity = player.velocity
        val diffVelocity = modifiedVelocity.subtract(originalVelocity) // チートによる理想値との差
        val originalSpeed = originalVelocity.horizontalLength()
        val diffSpeed = diffVelocity.horizontalLength()
        val diffMultiplier = diffSpeed / originalSpeed
        player.velocity =
            if (diffMultiplier > dynamicToleranceMultiplier) {
                val modifiedVelocity =
                    originalVelocity.add(diffVelocity.multiply(diffMultiplier / dynamicToleranceMultiplier))
                modifiedVelocity
            } else {
                modifiedVelocity
            }
    }
}
