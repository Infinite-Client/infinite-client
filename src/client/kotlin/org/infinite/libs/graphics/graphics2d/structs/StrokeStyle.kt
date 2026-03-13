package org.infinite.libs.graphics.graphics2d.structs

data class StrokeStyle(
    var color: Int = 0,
    var width: Float = 0f,
    var lineCap: LineCap = LineCap.Butt,
    var lineJoin: LineJoin = LineJoin.Miter,
    var enabledGradient: Boolean = true,
)
