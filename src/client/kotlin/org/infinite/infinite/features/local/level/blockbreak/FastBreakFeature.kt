package org.infinite.infinite.features.local.level.blockbreak

import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.phys.Vec3
import org.infinite.infinite.features.local.level.LocalLevelCategory
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.mixin.infinite.features.local.level.blockbreak.MultiPlayerGameModeAccessor

class FastBreakFeature : LocalFeature() {
    override val featureType = FeatureLevel.Utils
    override val categoryClass = LocalLevelCategory::class

    val safeMode by property(BooleanProperty(true))
    val interval by property(IntProperty(2, 0, 5))
    val thresholdPercentage by property(DoubleProperty(20.0, 0.0, 100.0, "%"))
    val thresholdTick by property(IntProperty(5, 1, 100))

    /**
     * 破壊パケットを送信する。
     * サーバー側の拒否を防ぐため、破壊直前にそのブロックを向いているパケットを送信する。
     */
    fun sendStopPacket(pos: BlockPos, side: Direction) {
        val player = player ?: return
        val connection = connection ?: return

        // 視線補正パケット (Silent Rotation)
        val center = Vec3.atCenterOf(pos)
        val diff = center.subtract(player.eyePosition)
        val dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z)
        val yaw = (Math.atan2(diff.z, diff.x) * 180.0 / Math.PI).toFloat() - 90.0f
        val pitch = (-(Math.atan2(diff.y, dist) * 180.0 / Math.PI)).toFloat()

        connection.send(ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), player.horizontalCollision))

        // 破壊完了パケット
        connection.send(ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, side))
    }

    fun shouldFastBreak(blockPos: BlockPos, progress: Float): Boolean {
        if (progress >= 1.0f || progress <= 0f) return false
        val player = player ?: return false
        val world = level ?: return false
        val state = world.getBlockState(blockPos)
        val progressPerTick = player.getDestroySpeed(state) /
            (state.getDestroySpeed(world, blockPos) * (if (player.hasCorrectToolForDrops(state)) 30f else 100f))
        if (progressPerTick <= 0) return false
        val remainTick = (1.0 - progress) / progressPerTick
        return (remainTick < thresholdTick.value.toDouble() && (1.0 - progress) < thresholdPercentage.value / 100.0)
    }
}
