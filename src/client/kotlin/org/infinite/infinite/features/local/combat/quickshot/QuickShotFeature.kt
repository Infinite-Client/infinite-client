package org.infinite.infinite.features.local.combat.quickshot

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.item.Items
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.IntProperty

class QuickShotFeature : LocalFeature() {
    override val featureType = FeatureLevel.Utils

    // チャージ完了からのオフセット (正の値で遅延、負の値で早期発射)
    private val intervalShift by property(IntProperty(0, -20, 20))

    override fun onStartTick() {
        val player = player ?: return

        // 1. プレイヤーがアイテムを使用中か確認
        if (!player.isUsingItem) return

        val itemStack = player.useItem
        val useDuration = itemStack.getUseDuration(player)

        // 2. 現在のチャージ経過時間を取得
        val currentDuration = useDuration - player.useItemRemainingTicks

        // 3. 各アイテムの目標チャージ時間に intervalShift を適用して判定
        val shouldRelease = when (itemStack.item) {
            Items.BOW -> {
                val target = 20 + intervalShift.value
                currentDuration >= target
            }

            Items.TRIDENT -> {
                val target = 10 + intervalShift.value
                currentDuration >= target
            }

            else -> false
        }

        // 4. 条件を満たしたら実行
        if (shouldRelease) {
            releaseBow()
        }
    }

    private fun releaseBow() {
        val player = player ?: return
        val connection = connection ?: return

        // サーバーに「手を離した」ことを通知
        connection.send(
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                Direction.DOWN,
            ),
        )

        // クライアント側の使用状態を停止
        player.stopUsingItem()
    }
}
