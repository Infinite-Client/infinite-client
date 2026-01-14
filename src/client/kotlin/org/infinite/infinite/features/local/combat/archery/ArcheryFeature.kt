package org.infinite.infinite.features.local.combat.archery

import io.netty.channel.ChannelFutureListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.item.Items
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.archery.projectile.ArrowProjectile
import org.infinite.infinite.features.local.combat.throwable.projectile.ThrowableProjectile
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.minecraft.projectile.AbstractProjectile
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class ArcheryFeature : LocalFeature() {
    override val featureType = FeatureLevel.Extend
    private var analysis: AbstractProjectile.TrajectoryAnalysis? = null

    fun handleWrappedLaunch(
        originalPacket: Packet<*>,
        listener: ChannelFutureListener?,
        flush: Boolean,
        ci: CallbackInfo,
        sendCall: (Packet<*>, ChannelFutureListener?, Boolean) -> Unit,
    ) {
        val player = player ?: return sendCall(originalPacket, listener, flush)

        val hand = if (originalPacket is ServerboundUseItemPacket) originalPacket.hand else player.usedItemHand
        val itemStack = player.getItemInHand(hand)

        val isBow = itemStack.item == Items.BOW
        val isCrossbow = itemStack.item == Items.CROSSBOW
        val isTrident = itemStack.item == Items.TRIDENT
        if (!isBow && !isCrossbow && !isTrident) {
            return sendCall(originalPacket, listener, flush)
        }

        // アイテムに応じた解析結果の取得
        val currentAnalysis = if (isTrident) {
            ThrowableProjectile.analyze()
        } else {
            ArrowProjectile.analyze()
        } ?: return sendCall(originalPacket, listener, flush)

        if (onlyWhenLockOn.value && !InfiniteClient.localFeatures.combat.lockOnFeature.isEnabled()) {
            return sendCall(originalPacket, listener, flush)
        }
        if (!ignoreTerrain.value && currentAnalysis.status == AbstractProjectile.PathStatus.Obstructed) {
            return sendCall(originalPacket, listener, flush)
        }

        // 3. エイムパケットの送信 (サーバー側に「今この方向を向いた」と思わせる)
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

        // 4. 発射パケットの構築
        // 重要: originalPacketのhandをそのまま使い、角度だけ計算後のものに差し替える
        val finalPacket = if (originalPacket is ServerboundUseItemPacket) {
            ServerboundUseItemPacket(
                originalPacket.hand, // 元のパケットが指定した方の手を使う
                originalPacket.sequence,
                currentAnalysis.yRot.toFloat(),
                currentAnalysis.xRot.toFloat(),
            )
        } else {
            originalPacket
        }

        sendCall(finalPacket, listener, false)

        // 5. 視線の復元 (クライアント側の見た目がガクガクしないようにする)
        sendCall(
            ServerboundMovePlayerPacket.Rot(
                player.yRot,
                player.xRot,
                player.onGround(),
                player.horizontalCollision,
            ),
            null,
            true,
        )

        ci.cancel() // 元のパケット送信をキャンセル
    }

    override fun onStartUiRendering(graphics2D: Graphics2D) {
        val result = ArrowProjectile.analyze() ?: return
        this.analysis = result
        ArrowProjectile.renderTrajectoryUI(
            graphics2D,
            result,
            InfiniteClient.theme.colorScheme.accentColor,
            InfiniteClient.theme.colorScheme.foregroundColor,
        )
    }

    val simulationMaxSteps by property(IntProperty(100, 20, 200))
    val simulationPrecision by property(IntProperty(20, 10, 64))
    val maxReach by property(IntProperty(128, 16, 256))
    val ignoreTerrain by property(BooleanProperty(false))
    val onlyWhenLockOn by property(BooleanProperty(false))
}
