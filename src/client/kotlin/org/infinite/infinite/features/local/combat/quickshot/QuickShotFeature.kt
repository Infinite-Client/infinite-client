package org.infinite.infinite.features.local.combat.quickshot

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.item.Items
import org.infinite.libs.core.features.feature.LocalFeature

class QuickShotFeature : LocalFeature() {
    override val featureType = FeatureType.Utils
    override fun onStartTick() {
        val player = player ?: return

        // 1. プレイヤーがアイテムを使用中（右クリック押しっぱなし）か確認
        if (!player.isUsingItem) return

        // 2. 使用中のアイテムが「弓」であることを確認
        val itemStack = player.useItem
        if (itemStack.`is`(Items.BOW)) {
            // 3. チャージ時間を取得 (弓の最大チャージは通常20 ticks)
            val useDuration = player.ticksUsingItem

            // 20ティック以上チャージされていたら即座に放つ
            // ※ラグを考慮して19〜20で調整するとよりスムーズになる場合があります
            if (useDuration >= 20) {
                releaseBow()
            }
        }
    }

    private fun releaseBow() {
        // サーバー側に「右クリックを離した」ことを通知するパケットを送信
        // これにより、クライアント側で指を離さなくても矢が放たれます
        connection?.send(
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                Direction.DOWN,
            ),
        )

        // クライアント側の内部状態もリセット（アニメーションの同期など）
        player?.stopUsingItem()
    }
}
