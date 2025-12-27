package org.infinite.libs.graphics.graphics2d.system

import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle

data class PathSegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val style: StrokeStyle,
)
