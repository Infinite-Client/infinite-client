package org.infinite.infinite.features.local.combat.archery

import io.netty.channel.ChannelFutureListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.archery.projectile.ArrowProjectile
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.log.LogSystem

class ArcheryFeature : LocalFeature() {
    private val arrowProjectile = ArrowProjectile()
    override val featureType = FeatureType.Extend

    /**
     * 発射パケットをラップし、パケットレベルでの視点偽装を行う
     */
    /**
     * 発射パケットをラップし、パケットレベルでの視点偽装を行う
     */
    fun handleWrappedLaunch(
        originalPacket: Packet<*>,
        listener: ChannelFutureListener?,
        flush: Boolean,
        sendCall: (Packet<*>, ChannelFutureListener?, Boolean) -> Unit,
    ) {
        val player = this@ArcheryFeature.player ?: return sendCall(originalPacket, listener, flush)

        // --- 1. オプションチェック: ロックオン時のみ実行 ---
        if (onlyWhenLockOn.value) {
            val lockOnFeature = InfiniteClient.localFeatures.combat.lockOnFeature
            if (!lockOnFeature.isEnabled()) {
                LogSystem.log("Lock-on feature is not enabled.")
                return sendCall(originalPacket, listener, flush)
            }
        }

        // 弾道分析を実行
        val analysis = arrowProjectile.analyze() ?: return sendCall(originalPacket, listener, flush)

        // --- 2. オプションチェック: 遮蔽物確認 ---
        // ignoreTerrain が false の時のみ、Obstructed（遮蔽あり）をチェックする
        val isBlocked = !ignoreTerrain.value && analysis.status == ArrowProjectile.PathStatus.Obstructed

        if (!isBlocked) {
            // サーバー側の向きを計算した角度に上書き
            val aimPacket = ServerboundMovePlayerPacket.Rot(
                analysis.yRot.toFloat(),
                analysis.xRot.toFloat(),
                player.onGround(),
                player.horizontalCollision,
            )
            sendCall(aimPacket, null, false)

            // 本来の発射パケットを送信
            sendCall(originalPacket, listener, false)

            // クライアント視点に戻す
            val restorePacket = ServerboundMovePlayerPacket.Rot(
                player.yRot,
                player.xRot,
                player.onGround(),
                player.horizontalCollision,
            )
            sendCall(restorePacket, null, flush)

            LogSystem.log("Silent sequence completed (Status: ${analysis.status}).")
        } else {
            // 遮蔽されている場合は、通常のパケットとして送信
            LogSystem.log("Shot cancelled/normal due to obstruction.")
            sendCall(originalPacket, listener, flush)
        }
    }

    val simulationMaxSteps by property(IntProperty(100, 20, 200))
    val simulationPrecision by property(IntProperty(20, 10, 64))
    val maxReach by property(IntProperty(128, 16, 256))
    val ignoreTerrain by property(BooleanProperty(false))
    val onlyWhenLockOn by property(BooleanProperty(false))
}
