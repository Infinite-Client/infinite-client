package org.infinite.features.rendering.detailinfo

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object TimeFormatter {
    fun formatTime(seconds: Double): String =
        if (seconds >= 60) {
            val minutes = (seconds / 60).toInt()
            val remainingSeconds = seconds % 60
            if (remainingSeconds >= 1) {
                "%dmin %.1fs".format(minutes, remainingSeconds)
            } else {
                "%dmin".format(minutes)
            }
        } else {
            "%.1fs".format(seconds)
        }

    fun getBreakingTimeText(
        progress: Float,
        client: Minecraft,
    ): Component {
        val player = client.player ?: return Component.empty()
        val world = client.level ?: return Component.empty()
        val interactionManager = client.gameMode ?: return Component.empty()

        val blockPos = interactionManager.destroyBlockPos
        val blockState = client.level?.getBlockState(blockPos)
        val destroySpeed = blockState?.getDestroyProgress(player, world, blockPos) ?: 0.0f

        if (destroySpeed <= 0.0001f) return Component.literal("Indestructible")

        val totalTicks = 1.0f / destroySpeed
        val remainingTicks = (1.0f - progress) * totalTicks
        val totalSeconds = totalTicks / 20.0
        val remainingSeconds = remainingTicks / 20.0

        return Component.literal(
            "Time: ${
                formatTime(remainingSeconds)
            } / ${
                formatTime(totalSeconds)
            }",
        )
    }
}
