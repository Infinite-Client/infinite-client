package org.infinite.infinite.features.server.info

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ServerData
import org.infinite.InfiniteClient
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting

class ServerInfo : ConfigurableFeature() {
    override val settings: List<FeatureSetting<*>> = emptyList()
    override val level: FeatureLevel = FeatureLevel.Utils
    override val preRegisterCommands: List<String> = emptyList()
    override val togglable: Boolean = false

    fun show(): Int {
        val info = getCurrentServerInfo()
        if (info != null) {
            var serverInfoText = ""
            serverInfoText += "Name: ${info.name}\n"
            serverInfoText += "Address: ${info.ip}\n"
            serverInfoText += "Version: ${info.version.string}\n"
            serverInfoText += "Protocol Version: ${info.protocol}\n"
            serverInfoText += "Ping: ${info.ping}ms\n"
            info.players?.let { players ->
                serverInfoText += "Players: ${players.online}/${players.max}\n"
            }
            serverInfoText += "Resource Pack Policy: ${info.resourcePackStatus.name}\n"
            serverInfoText += "Server Type: ${info.type().name}\n"
            InfiniteClient.log("\n$serverInfoText")
        } else {
            InfiniteClient.error("Failed to get Server Info")
        }
        return 1
    }

    override fun onEnabled() {
        disable()
    }

    override fun registerCommands(builder: LiteralArgumentBuilder<FabricClientCommandSource>) {
        builder.then(
            ClientCommandManager.literal("show").executes { _ -> show() },
        )
    }

    fun getCurrentServerInfo(): ServerData? {
        val client = Minecraft.getInstance()
        return client.currentServer
    }
}
