package org.infinite.libs.graphics.text

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.font.FontSet
import org.infinite.libs.graphics.text.font.FontSetProvider

fun fromFontSet(fontSet: FontSet): Font {
    val provider = FontSetProvider(fontSet)
    return Font(provider)
}
