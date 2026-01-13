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
import org.infinite.utils.alpha
import kotlin.math.PI

class ArcheryFeature : LocalFeature() {
    private val arrowProjectile = ArrowProjectile()
    override val featureType = FeatureType.Extend
    fun handleWrappedLaunch(
        originalPacket: Packet<*>,
        listener: ChannelFutureListener?,
        flush: Boolean,
        sendCall: (Packet<*>, ChannelFutureListener?, Boolean) -> Unit,
    ) {
        val player = this@ArcheryFeature.player ?: return sendCall(originalPacket, listener, flush)
        val item = player.mainHandItem

        val isCrossbow = item.item == Items.CROSSBOW
        if (isCrossbow && !CrossbowItem.isCharged(item)) {
            return sendCall(originalPacket, listener, flush)
        }

        val analysis = analysis ?: return sendCall(originalPacket, listener, flush)

        // 条件チェック
        if (onlyWhenLockOn.value && !InfiniteClient.localFeatures.combat.lockOnFeature.isEnabled()) {
            return sendCall(originalPacket, listener, flush)
        }
        if (!ignoreTerrain.value && analysis.status == ArrowProjectile.PathStatus.Obstructed) {
            return sendCall(originalPacket, listener, flush)
        }

        /* --- サイレントエイム・シーケンス --- */

        // 1. エイムパケット（サーバーの物理的な向きを同期）
        val aimPacket = ServerboundMovePlayerPacket.Rot(
            analysis.yRot.toFloat(),
            analysis.xRot.toFloat(),
            player.onGround(),
            player.horizontalCollision,
        )
        sendCall(aimPacket, null, false)

        // 2. 元のパケットの処理（回転情報を保持している場合は上書き）
        val finalPacket = if (originalPacket is ServerboundUseItemPacket) {
            // パケット自体の回転情報を偽装後の値で再構成する
            // ※コンストラクタを使用して新しいパケットを作成します
            ServerboundUseItemPacket(
                originalPacket.hand,
                originalPacket.sequence,
                analysis.yRot.toFloat(), // ここを上書き！
                analysis.xRot.toFloat(), // ここを上書き！
            )
        } else {
            originalPacket
        }

        sendCall(finalPacket, listener, false)

        // 3. 復元パケット
        val restorePacket = ServerboundMovePlayerPacket.Rot(
            player.yRot,
            player.xRot,
            player.onGround(),
            player.horizontalCollision,
        )
        sendCall(restorePacket, null, true)
    }

    private var analysis: ArrowProjectile.TrajectoryAnalysis? = null
    override fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D {
        val player = player ?: return graphics2D
        analysis = arrowProjectile.analyze() ?: return graphics2D
        val analysis = analysis ?: return graphics2D
        // 1. 地形衝突を考慮した「実際の着弾点」を使用
        val targetPos = analysis.hitPos
        val screenPos = graphics2D.projectWorldToScreen(targetPos) ?: return graphics2D

        val x = screenPos.first.toFloat()
        val y = screenPos.second.toFloat()
        val distance = player.eyePosition.distanceTo(targetPos)

        val accentColor = InfiniteClient.theme.colorScheme.accentColor
        val foregroundColor = InfiniteClient.theme.colorScheme.foregroundColor

        // --- 2. 誤差円の計算 (距離とFOVを考慮) ---
        // 遠距離での巨大化を防ぐため、さらに係数を調整
        val errorRadius = (distance.toFloat() * 0.4f).coerceIn(4f, 30f)

        graphics2D.beginPath()
        graphics2D.strokeStyle.color = when (analysis.status) {
            ArrowProjectile.PathStatus.Obstructed -> accentColor.alpha(40) // 遮蔽時は薄く
            ArrowProjectile.PathStatus.Uncertain -> accentColor.alpha(160)
            else -> accentColor.alpha(100)
        }
        graphics2D.strokeStyle.width = 1.2f
        graphics2D.arc(x, y, errorRadius, 0f, (PI * 2).toFloat())
        graphics2D.strokePath()

        // --- 3. 視認性重視のテキスト描画 (アウトライン風) ---
        val distText = String.format("%.1f m", distance)
        graphics2D.textStyle.size = 11f
        graphics2D.textStyle.font = "infinite_regular"

        // 黒い縁取りを描画
        graphics2D.fillStyle = net.minecraft.world.item.DyeColor.BLACK.textColor.alpha(200)
        for (offX in -1..1) {
            for (offY in -1..1) {
                if (offX == 0 && offY == 0) continue
                graphics2D.textCentered(distText, x + offX, y + errorRadius + 10f + offY)
            }
        }

        // メインテキスト
        graphics2D.fillStyle = foregroundColor
        graphics2D.textCentered(distText, x, y + errorRadius + 10f)

        return graphics2D
    }

    val simulationMaxSteps by property(IntProperty(100, 20, 200))
    val simulationPrecision by property(IntProperty(20, 10, 64))
    val maxReach by property(IntProperty(128, 16, 256))
    val ignoreTerrain by property(BooleanProperty(false))
    val onlyWhenLockOn by property(BooleanProperty(false))
}
