package org.infinite.libs.core.features.feature

import org.infinite.libs.core.TickInterface
import org.infinite.libs.core.features.Feature

open class GlobalFeature :
    Feature(),
    TickInterface {
    fun onInitialized() {
    }

    fun onShutdown() {
    }

    override fun onStartTick() {
    }

    override fun onEndTick() {
    }
}
