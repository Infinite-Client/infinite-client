package org.infinite.libs.graphics.graphics2d.structs

import net.minecraft.client.Minecraft

class TextStyle(
    val shadow: Boolean = false,
    val size: Float = Minecraft.getInstance().font.lineHeight.toFloat(),
    val font: String = "default",
)
