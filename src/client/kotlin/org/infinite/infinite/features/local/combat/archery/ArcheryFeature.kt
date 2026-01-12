package org.infinite.infinite.features.local.combat.archery

import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.item.BowItem
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.Graphics2D

class ArcheryFeature : LocalFeature() {
    override val featureType = FeatureType.Utils

    // 設定値
    private var maxSimulationTicks = 100
    private val gravity = 0.05
    private val airResistance = 0.99

    override fun onEndTick() {
        val player = minecraft.player ?: return
        val stack = player.useItem

        // 弓を使用中かチェック
        if (stack.item is BowItem) {
            val ticksUsed = player.ticksUsingItem
            val power = BowItem.getPowerForTime(ticksUsed)

            if (power > 0.1) {
                // 1. ターゲット（注視点）の取得
                val hitResult = player.pick(100.0, 0f, false)
                val targetPos = hitResult.location
            }
        }
    }

    override fun onEndUiRendering(graphics2D: Graphics2D): Graphics2D {
        val player = minecraft.player ?: return graphics2D
        val stack = player.useItem

        if (stack.item is BowItem) {
            val power = BowItem.getPowerForTime(player.ticksUsingItem) * 3.0 // 初速
            drawTrajectory(graphics2D, player, power)
        }
        return graphics2D
    }

    /**
     * 弾道を計算して画面上に投影・描画する
     */
    private fun drawTrajectory(g: Graphics2D, player: LocalPlayer, velocity: Double) {
        var pos = player.getEyePosition(g.gameDelta)
        // プレイヤーの視線方向から初速度ベクトルを計算
        var motion = player.lookAngle.scale(velocity)

        g.strokeStyle.color = 0xFF00FF00.toInt() // 緑色の予測線
        g.beginPath()

        var prevScreenPos: Pair<Double, Double>? = null

        for (i in 0 until maxSimulationTicks) {
            // 世界座標を画面座標に変換
            val screenPos = g.projectWorldToScreen(pos)

            if (screenPos != null) {
                if (prevScreenPos == null) {
                    g.moveTo(screenPos.first.toFloat(), screenPos.second.toFloat())
                } else {
                    g.lineTo(screenPos.first.toFloat(), screenPos.second.toFloat())
                }
            }
            prevScreenPos = screenPos

            // 物理シミュレーション (1 tick 分)
            pos = pos.add(motion)
            motion = motion.scale(airResistance)
            motion = motion.add(0.0, -gravity, 0.0)

            // ブロック衝突判定（簡易版）
            if (minecraft.level?.getBlockState(net.minecraft.core.BlockPos.containing(pos))?.isAir == false) {
                // 着弾点に円を描画
                if (screenPos != null) {
                    g.fillStyle = 0xFFFF0000.toInt() // 赤
                    g.fillRect(screenPos.first.toFloat() - 2, screenPos.second.toFloat() - 2, 4f, 4f)
                }
                break
            }
        }
        g.strokePath()
    }
}
