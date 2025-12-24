package org.infinite.features.local.rendering.hello

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.log.LogSystem

class HelloFeature : LocalFeature() {
    override fun onConnected() {
        LogSystem.log("Hello, World!")
    }

    override fun onStartTick() {
        LogSystem.log("Tick")
    }

    override fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D {
        graphics2D.strokeStyle = StrokeStyle(0xFFFFFFFF.toInt(), 1.0)
        graphics2D.strokeRect(50, 50, 100, 100)
        return graphics2D
    }
}
