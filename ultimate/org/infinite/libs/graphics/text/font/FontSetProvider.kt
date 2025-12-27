package org.infinite.libs.graphics.text.font

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GlyphSource
import net.minecraft.client.gui.font.FontSet
import net.minecraft.client.gui.font.glyphs.EffectGlyph
import net.minecraft.network.chat.FontDescription

class FontSetProvider(private val fontSet: FontSet) : Font.Provider {
    override fun glyphs(fontDescription: FontDescription): GlyphSource {
        return fontSet.source(false)
    }

    override fun effect(): EffectGlyph {
        return fontSet.whiteGlyph()
    }
}
