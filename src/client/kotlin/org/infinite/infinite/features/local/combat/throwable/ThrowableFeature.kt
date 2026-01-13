package org.infinite.infinite.features.local.combat.throwable

import io.netty.channel.ChannelFutureListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
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

    /**
     * ThrowableProjectileからの情報を元に、パケットをサイレントエイム化する
     */
    fun handleWrappedLaunch(
        originalPacket: Packet<*>,
        listener: ChannelFutureListener?,
        flush: Boolean,
        ci: CallbackInfo,
        sendCall: (Packet<*>, ChannelFutureListener?, Boolean) -> Unit,
    ) {
        val player = this.player ?: return sendCall(originalPacket, listener, flush)

        // 解析結果がない場合は通常通り送信
        val currentAnalysis = analysis ?: return sendCall(originalPacket, listener, flush)

        // 条件チェック: ロックオン時のみや地形無視設定など（必要に応じて追加）
        if (onlyWhenLockOn.value && !InfiniteClient.localFeatures.combat.lockOnFeature.isEnabled()) {
            return sendCall(originalPacket, listener, flush)
        }

        // 1. エイム（サーバーの同期）
        val aimPacket = ServerboundMovePlayerPacket.Rot(
            currentAnalysis.yRot.toFloat(),
            currentAnalysis.xRot.toFloat(),
            player.onGround(),
            player.horizontalCollision,
        )
        sendCall(aimPacket, null, false)

        // 2. 発射パケット (UseItemPacket内の角度を上書き)
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
        val restorePacket = ServerboundMovePlayerPacket.Rot(
            player.yRot,
            player.xRot,
            player.onGround(),
            player.horizontalCollision,
        )
        sendCall(restorePacket, null, true)
        ci.cancel()
    }

    override fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D {
        val analysisResult = throwableProjectile.analyze() ?: return graphics2D
        this.analysis = analysisResult // パケット送信時用に保持

        return throwableProjectile.renderTrajectoryUI(
            graphics2D,
            analysisResult,
            InfiniteClient.theme.colorScheme.accentColor,
            InfiniteClient.theme.colorScheme.foregroundColor,
        )
    }

    val simulationMaxSteps by property(IntProperty(100, 20, 200))
    val simulationPrecision by property(IntProperty(20, 10, 64))
    val maxReach by property(IntProperty(64, 16, 128))
    val onlyWhenLockOn by property(BooleanProperty(false))
}
