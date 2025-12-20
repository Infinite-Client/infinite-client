package org.infinite.features.local.rendering.hello

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.log.LogSystem

class HelloFeature : LocalFeature() {
    override fun onConnected() {
        LogSystem.log("Hello, World!")
    }

    override fun onStartTick() {
        LogSystem.log("Tick")
    }
}
