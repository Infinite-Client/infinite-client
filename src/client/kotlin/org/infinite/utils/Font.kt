package org.infinite.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import org.infinite.libs.graphics.graphics2d.text.IModernFontManager
import org.infinite.libs.graphics.text.fromFontSet
import org.infinite.mixin.graphics.MinecraftAccessor

fun Font(name: String): Font {
    val minecraft = Minecraft.getInstance() as MinecraftAccessor
    val fontManager = minecraft.fontManager as IModernFontManager
    val fontSet = fontManager.`infinite$fontSetFromIdentifier`(name)
    return fromFontSet(fontSet)
}
