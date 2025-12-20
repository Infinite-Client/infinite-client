package org.infinite.libs.core.features.feature

import org.infinite.libs.core.TickInterface
import org.infinite.libs.core.features.Feature

open class LocalFeature :
    Feature(),
    TickInterface {
    open fun onConnected() {
    }

    open fun onDisconnected() {
    }

    override fun onStartTick() {
    }

    override fun onEndTick() {
    }
}
