package org.infinite.mixin.graphics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.infinite.UltimateClient;
import org.infinite.libs.graphics.graphics2d.text.IModernFontManager;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontMixin implements StringSplitter.WidthProvider {

    @Shadow
    protected abstract GlyphSource getGlyphSource(FontDescription fontDescription);

    // 自身(WidthProvider)を渡して、独自幅計算用のSplitterを保持
    @Unique
    private final StringSplitter modernStringSplitter = new StringSplitter(this);

    /**
     * String (プレーンテキスト) の幅計算をフック
     */
    @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    public void onStringWidth(String string, CallbackInfoReturnable<Integer> cir) {
        if (isUltimateFontEnabled()) {
            // StringSplitterのstringWidthはfloatを返すため、intにキャスト
            cir.setReturnValue((int) Math.ceil(this.modernStringSplitter.stringWidth(string)));
        }
    }

    /**
     * FormattedText (Component等のベース) の幅計算をフック
     */
    @Inject(
            method = "width(Lnet/minecraft/network/chat/FormattedText;)I",
            at = @At("HEAD"),
            cancellable = true)
    public void onFormattedTextWidth(
            FormattedText formattedText, CallbackInfoReturnable<Integer> cir) {
        if (isUltimateFontEnabled()) {
            cir.setReturnValue((int) Math.ceil(this.modernStringSplitter.stringWidth(formattedText)));
        }
    }

    /**
     * FormattedCharSequence (描画直前の整形済みテキスト) の幅計算をフック
     */
    @Inject(
            method = "width(Lnet/minecraft/util/FormattedCharSequence;)I",
            at = @At("HEAD"),
            cancellable = true)
    public void onCharSequenceWidth(
            FormattedCharSequence sequence, CallbackInfoReturnable<Integer> cir) {
        if (isUltimateFontEnabled()) {
            cir.setReturnValue((int) Math.ceil(this.modernStringSplitter.stringWidth(sequence)));
        }
    }

    /**
     * StringSplitter.WidthProvider の実装 ここで返される値が StringSplitter の計算の基礎になります
     */
    @Override
    public float getWidth(int codePoint, @NonNull Style style) {
        MinecraftAccessor client = (MinecraftAccessor) Minecraft.getInstance();
        IModernFontManager fontManager = (IModernFontManager) client.getFontManager();
        @SuppressWarnings("resource")
        FontSet fontSet = fontManager.ultimate$fontSetFromStyle(style);
        if (fontSet != null) {
            return fontSet.source(false).getGlyph(codePoint).info().getAdvance(false);
        } else {
            return this.getGlyphSource(style.getFont())
                    .getGlyph(codePoint)
                    .info()
                    .getAdvance(style.isBold());
        }
    }

    @Unique
    private boolean isUltimateFontEnabled() {
        return UltimateClient.INSTANCE
                .getGlobalFeatures()
                .getRendering()
                .getUltimateFontFeature()
                .isEnabled();
    }
}
