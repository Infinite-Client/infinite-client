package org.infinite.infinite.features.local.combat.archery

import io.netty.channel.ChannelFutureListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.Items
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.archery.projectile.ArrowProjectile
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.minecraft.projectile.AbstractProjectile
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class ArcheryFeature : LocalFeature() {
    private val arrowProjectile = ArrowProjectile(this)
    override val featureType = FeatureType.Extend
    private var analysis: AbstractProjectile.TrajectoryAnalysis? = null

    fun handleWrappedLaunch(
        originalPacket: Packet<*>,
        listener: ChannelFutureListener?,
        flush: Boolean,
        ci: CallbackInfo,
        sendCall: (Packet<*>, ChannelFutureListener?, Boolean) -> Unit,
    ) {
        val player = player ?: return sendCall(originalPacket, listener, flush)
        val item = player.mainHandItem

        // アイテム・チャージチェック
        if (item.item == Items.CROSSBOW && !CrossbowItem.isCharged(item)) {
            return sendCall(
                originalPacket,
                listener,
                flush,
            )
        }
        val currentAnalysis = analysis ?: return sendCall(originalPacket, listener, flush)

        // 条件チェック
        if (onlyWhenLockOn.value && !InfiniteClient.localFeatures.combat.lockOnFeature.isEnabled()) {
            return sendCall(
                originalPacket,
                listener,
                flush,
            )
        }
        if (!ignoreTerrain.value && currentAnalysis.status == AbstractProjectile.PathStatus.Obstructed) {
            return sendCall(
                originalPacket,
                listener,
                flush,
            )
        }

        // 1. エイムパケット
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

        // 2. 発射パケット (UseItemのみ上書き)
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

        // 3. 復元
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
        ci.cancel()
    }

    override fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D {
        val result = arrowProjectile.analyze() ?: return graphics2D
        this.analysis = result

        return arrowProjectile.renderTrajectoryUI(
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
