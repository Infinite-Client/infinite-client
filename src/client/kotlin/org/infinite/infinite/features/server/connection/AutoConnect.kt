package org.infinite.infinite.features.server.connection

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import org.infinite.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting

class AutoConnect : ConfigurableFeature() {
    var lastServer: ServerData? = null

    override fun onStart() {
        lastServer = Minecraft.getInstance().currentServer
    }

    fun joinLastServer(mpScreen: JoinMultiplayerScreen) {
        val lastServer = lastServer ?: return
        mpScreen.join(lastServer)
    }

    fun reconnect(prevScreen: Screen?) {
        val prevScreen = prevScreen ?: return
        val lastServer = lastServer ?: return
        ConnectScreen.startConnecting(
            prevScreen,
            Minecraft.getInstance(),
            ServerAddress.parseString(lastServer.ip),
            lastServer,
            false,
            null,
        )
    }

    val waitTicks =
        FeatureSetting.IntSetting(
            "WaitTicks",
            40,
            10,
            300,
        )
    override val settings: List<FeatureSetting<*>> = listOf(waitTicks)
    override val level: FeatureLevel = FeatureLevel.Utils
}
