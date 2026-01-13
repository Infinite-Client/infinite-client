package org.infinite.infinite.features.local.combat.archery

import io.netty.channel.ChannelFutureListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import org.infinite.infinite.features.local.combat.archery.projectile.ArrowProjectile
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.log.LogSystem

class ArcheryFeature : LocalFeature() {
    private val arrowProjectile = ArrowProjectile()
    override val featureType = FeatureType.Extend

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
        val analysis = arrowProjectile.analyze() ?: return sendCall(originalPacket, listener, flush)
        val diffX = player.xRot - analysis.xRot
        val diffY = player.yRot - analysis.yRot
        LogSystem.log("HOOKED!: ${"%.1f".format(diffX)}, ${"%.1f".format(diffY)}")
        if (analysis.status != ArrowProjectile.PathStatus.Obstructed) {
            // 1. サーバー側の向きを計算した角度に上書き
            val aimPacket = ServerboundMovePlayerPacket.Rot(
                analysis.yRot.toFloat(),
                analysis.xRot.toFloat(),
                player.onGround(),
                player.horizontalCollision,
            )
            sendCall(aimPacket, null, false) // まだ flush しない

            // 2. 本来の発射パケットを送信
            sendCall(originalPacket, listener, false)

            // 3. サーバー側の向きを即座に元のクライアント視点に戻す
            val restorePacket = ServerboundMovePlayerPacket.Rot(
                player.yRot,
                player.xRot,
                player.onGround(),
                player.horizontalCollision,
            )
            sendCall(restorePacket, null, flush) // ここでまとめて送信

            LogSystem.log("Silent sequence completed.")
        } else {
            sendCall(originalPacket, listener, flush)
        }
    }

    val simulationMaxSteps by property(IntProperty(100, 20, 200))
    val simulationPrecision by property(IntProperty(20, 10, 64))
    val maxReach by property(IntProperty(128, 16, 256))
}
