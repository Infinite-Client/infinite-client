package org.infinite.libs.graphics.graphics3d.structs

import net.minecraft.world.phys.Vec3

data class TexturedVertex(
    val position: Vec3,
    val u: Float,
    val v: Float,
    val color: Int = 0xFFFFFFFF.toInt(),
)
