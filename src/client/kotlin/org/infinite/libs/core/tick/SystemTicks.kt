package org.infinite.libs.core.tick

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.infinite.InfiniteClient
import org.infinite.libs.core.TickInterface

object SystemTicks : TickInterface {
    override fun onStartTick() {
    }

    override fun onEndTick() {
        InfiniteClient.localFeatures.keyMappingActions()
    }

    fun register() {
        ClientTickEvents.START_CLIENT_TICK.register { _ -> onStartTick() }
        ClientTickEvents.END_CLIENT_TICK.register { _ -> onEndTick() }
    }
}
