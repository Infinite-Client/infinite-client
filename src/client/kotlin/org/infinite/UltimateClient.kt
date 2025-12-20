package org.infinite

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import org.infinite.libs.core.features.categories.GlobalFeatureCategories
import org.infinite.libs.core.features.categories.LocalFeatureCategories
import org.infinite.libs.log.LogSystem

object UltimateClient : ClientModInitializer {
    val globalFeatureCategories = GlobalFeatureCategories()
    val localFeatureCategories = LocalFeatureCategories()

    override fun onInitializeClient() {
        LogSystem.init()
        globalFeatureCategories.onInitialized()

        // サーバー接続時 (ログイン成功後)
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            localFeatureCategories.onConnected()
        }

        // サーバー切断時
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            localFeatureCategories.onDisconnected()
        }

        // --- Shutdown (マイクラ終了時) ---
        ClientLifecycleEvents.CLIENT_STOPPING.register { _ ->
            globalFeatureCategories.onShutdown()
            localFeatureCategories.onShutdown()
        }
    }
}
