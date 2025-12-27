package org.infinite.infinite.features.rendering.font

import com.mojang.blaze3d.font.GlyphInfo
import net.minecraft.client.Minecraft
import net.minecraft.client.StringSplitter
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GlyphSource
import net.minecraft.client.gui.font.FontManager
import net.minecraft.client.gui.font.FontSet
import net.minecraft.client.gui.font.glyphs.BakedGlyph
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource

class HyperTextRenderer(
    val fontManager: FontManager,
) : Font(fontManager.anyGlyphs),
    StringSplitter.WidthProvider {
    init {
        this.splitter = StringSplitter(this)
    }

    override fun getWidth(
        codePoint: Int,
        style: Style,
    ): Float = this.getGlyph(codePoint, style).info()?.getAdvance(style.isBold) ?: 0f

    class HyperFonts(
        val regularIdentifier: Identifier,
        val italicIdentifier: Identifier,
        val boldIdentifier: Identifier,
        val italicBoldIdentifier: Identifier,
    )

    // カスタムフォントのIdentifier
    private var hyperFonts: HyperFonts =
        HyperFonts(
            Identifier.fromNamespaceAndPath("minecraft", "default"),
            Identifier.fromNamespaceAndPath("minecraft", "default"),
            Identifier.fromNamespaceAndPath("minecraft", "default"),
            Identifier.fromNamespaceAndPath("minecraft", "default"),
        )
    private val random = RandomSource.create()

    fun defineFont(hyperFonts: HyperFonts) {
        this.hyperFonts = hyperFonts
    }

    private fun getHyperFontIdentifier(style: Style): Identifier =
        when {
            style.isBold && style.isItalic -> hyperFonts.italicBoldIdentifier
            style.isBold -> hyperFonts.boldIdentifier
            style.isItalic -> hyperFonts.italicIdentifier
            else -> hyperFonts.regularIdentifier
        }

    class ZeroBoldOffsetMetrics(
        private val baseMetrics: GlyphInfo,
    ) : GlyphInfo by baseMetrics {
        override fun getAdvance(bold: Boolean): Float = super.getAdvance(bold)

        override fun getBoldOffset(): Float = 0.0f

        override fun getShadowOffset(): Float = super.getShadowOffset()
    }

    class CustomBakedGlyph(
        private val baseGlyph: BakedGlyph,
        metrics: GlyphInfo,
    ) : BakedGlyph by baseGlyph {
        private val customMetrics = metrics

        override fun info(): GlyphInfo = customMetrics
    }

    // HyperTextRenderer クラス内の変更
    override fun getGlyph(
        codePoint: Int,
        style: Style,
    ): BakedGlyph {
        if (!isEnabled) {
            return super.getGlyph(codePoint, style)
        }
        val fontId = getHyperFontIdentifier(style)
        val fontStorage: FontSet = fontManager.getFontSetRaw(fontId)
        val glyphProvider: GlyphSource = fontStorage.source(false)
        var bakedGlyph = glyphProvider.getGlyph(codePoint)
        if (style.isObfuscated && codePoint != 32) {
            val i = Mth.ceil(bakedGlyph.info().getAdvance(false))
            bakedGlyph = glyphProvider.getRandomGlyph(random, i)
        }
        if (style.isBold) {
            // 太字スタイルのグリフが取得できた場合、標準の二重描画を抑制する
            val zeroBoldMetrics = ZeroBoldOffsetMetrics(bakedGlyph.info())
            return CustomBakedGlyph(bakedGlyph, zeroBoldMetrics)
        }

        return bakedGlyph
    }

    private var isEnabled = false
    private val client: Minecraft
        get() = Minecraft.getInstance()

    private fun reinitChatHud() {
        val chatHud = client.gui.chat
        chatHud.refreshTrimmedMessages()
    }

    fun enable() {
        isEnabled = true
        reinitChatHud()
    }

    fun disable() {
        isEnabled = false
        reinitChatHud()
    }
}
