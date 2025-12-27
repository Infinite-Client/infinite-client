package org.infinite.libs.core.tick

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.infinite.UltimateClient
import org.infinite.libs.core.TickInterface

object SystemTicks : TickInterface {
    override fun onStartTick() {
    }

    override fun onEndTick() {
        UltimateClient.localFeatures.keyBindingActions()
    }

    fun register() {
        ClientTickEvents.START_CLIENT_TICK.register { _ -> onStartTick() }
        ClientTickEvents.END_CLIENT_TICK.register { _ -> onEndTick() }
    }
}
