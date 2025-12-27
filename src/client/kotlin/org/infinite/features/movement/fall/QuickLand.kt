package org.infinite.features.movement.fall

import net.minecraft.core.Direction
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.ClipContext.Block
import net.minecraft.world.level.ClipContext.Fluid
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.infinite.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting

class QuickLand : ConfigurableFeature(initialEnabled = false) {
    private val safeFallDistance =
        FeatureSetting.DoubleSetting(
            "SafeFallDistance",
            3.0, // Minecraftの落下ダメージ開始距離を参考にデフォルト値を設定
            0.0,
            10.0,
        )

    override val settings: List<FeatureSetting<*>> = listOf(safeFallDistance)
    var interval = 0

    override fun onTick() {
        if (interval > 0) {
            --interval
            return
        }
        val player = player ?: return
        val world = world ?: return

        // プレイヤーが地面にいる場合は何もしない
        if (player.onGround()) return

        // 上向きに移動している場合は何もしない
        if (player.deltaMovement.y > 0) return

        // 落下距離を計算
        // プレイヤーの現在のY座標と、最後に地面にいた時のY座標の差
        val fallDistance = player.fallDistance

        // SafeFallDistanceを下回っているかチェック
        if (fallDistance >= safeFallDistance.value) return

        // プレイヤーの足元から下方向へのRaycastで地面を検出
        val startVec = playerPos!!
        val endVec = startVec.add(0.0, -1.0, 0.0) // 1ブロック下までRaycast

        val hitResult =
            world.clip(
                ClipContext(
                    startVec,
                    endVec,
                    Block.OUTLINE,
                    Fluid.ANY,
                    player,
                ),
            )

        if (hitResult is BlockHitResult && hitResult.direction == Direction.UP) {
            // 地面との距離が1ブロック未満かチェック (Raycastがヒットした時点でほぼ1ブロック未満)
            // ここでは、ヒットしたブロックの上面のY座標を取得
            val groundY = hitResult.location.y
            // プレイヤーのY座標を地面のY座標に設定して即座に着地
            val v = velocity ?: return
            velocity = Vec3(v.x, groundY - player.y, v.z)
            interval = 5
        }
    }
}
