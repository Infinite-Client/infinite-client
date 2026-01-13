package org.infinite.infinite.features.local.combat.throwable

import io.netty.channel.ChannelFutureListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.item.ExperienceBottleItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.ThrowablePotionItem
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.throwable.projectile.ThrowableProjectile
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.minecraft.projectile.AbstractProjectile
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class ThrowableFeature : LocalFeature() {
    private val throwableProjectile = ThrowableProjectile(this)
    override val featureType = FeatureType.Extend

    private var analysis: AbstractProjectile.TrajectoryAnalysis? = null

    fun handleWrappedLaunch(
        originalPacket: Packet<*>,
        listener: ChannelFutureListener?,
        flush: Boolean,
        ci: CallbackInfo,
        sendCall: (Packet<*>, ChannelFutureListener?, Boolean) -> Unit,
    ) {
        val player = this.player ?: return

        // 1. 現在使用しようとしているアイテムを確認 (オフハンド対応)
        val hand = if (originalPacket is ServerboundUseItemPacket) originalPacket.hand else player.usedItemHand
        val itemStack = player.getItemInHand(hand)

        // 投擲アイテムでなければ何もしない
        if (!isThrowable(itemStack)) return

        // 2. 最新の解析結果を取得 (鮮度を優先)
        val currentAnalysis = throwableProjectile.analyze() ?: return

        // ロックオン時のみの制限チェック
        if (onlyWhenLockOn.value && !InfiniteClient.localFeatures.combat.lockOnFeature.isEnabled()) {
            return
        }

        // 3. サイレントエイム・パケットシーケンス
        // A. サーバー側の向きを補正
        sendCall(
            ServerboundMovePlayerPacket.Rot(
                currentAnalysis.yRot.toFloat(),
                currentAnalysis.xRot.toFloat(),
                player.onGround(),
                player.horizontalCollision,
            ),
            null,
            false,
        )

        // B. 発射パケット (角度情報を上書き)
        val finalPacket = if (originalPacket is ServerboundUseItemPacket) {
            ServerboundUseItemPacket(
                originalPacket.hand,
                originalPacket.sequence,
                currentAnalysis.yRot.toFloat(),
                currentAnalysis.xRot.toFloat(),
            )
        } else {
            originalPacket
        }
        sendCall(finalPacket, listener, false)

        // C. 元の向きに復元 (クライアント側のガクつき防止)
        sendCall(
            ServerboundMovePlayerPacket.Rot(
                player.yRot,
                player.xRot,
                player.onGround(),
                player.horizontalCollision,
            ),
            null,
            true, // ここでフラッシュしてまとめて送信
        )

        ci.cancel()
    }

    override fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D {
        val analysisResult = throwableProjectile.analyze() ?: return graphics2D
        this.analysis = analysisResult

        return throwableProjectile.renderTrajectoryUI(
            graphics2D,
            analysisResult,
            InfiniteClient.theme.colorScheme.accentColor,
            InfiniteClient.theme.colorScheme.foregroundColor,
        )
    }

    private fun isThrowable(stack: ItemStack): Boolean {
        val item = stack.item
        return item is SnowballItem || item is EggItem || item is EnderpearlItem ||
            item is ThrowablePotionItem || item is ExperienceBottleItem
    }

    val simulationMaxSteps by property(IntProperty(100, 20, 200))
    val simulationPrecision by property(IntProperty(20, 10, 64))
    val maxReach by property(IntProperty(64, 16, 128))
    val onlyWhenLockOn by property(BooleanProperty(false))
}
