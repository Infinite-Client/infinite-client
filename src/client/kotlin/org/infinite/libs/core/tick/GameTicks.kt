package org.infinite.libs.core.tick

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.infinite.libs.core.TickInterface
import org.infinite.libs.core.features.categories.LocalFeatureCategories
import org.infinite.libs.minecraft.input.InputSystem

class GameTicks(
    private val localFeatureCategories: LocalFeatureCategories,
) : TickInterface {
    override fun onStartTick() {
        InputSystem.tick()
        localFeatureCategories.onStartTick()
    }

    override fun onEndTick() {
        localFeatureCategories.onEndTick()
    }

    fun register() {
        ClientTickEvents.START_WORLD_TICK.register { _ -> onStartTick() }
        ClientTickEvents.END_WORLD_TICK.register { _ -> onEndTick() }
    }
}
