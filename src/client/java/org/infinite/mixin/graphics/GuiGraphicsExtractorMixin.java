package org.infinite.mixin.graphics;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.infinite.InfiniteClient;
import org.infinite.libs.graphics.graphics2d.text.IModernFontManager;
import org.infinite.libs.graphics.text.FontFromFontSetKt;
import org.infinite.libs.graphics.text.ModernTextRenderer;
import org.infinite.libs.graphics.text.font.StyleExtractor;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {
  @Shadow @Final private Minecraft minecraft;
  @Shadow @Final public GuiRenderState guiRenderState;
  @Shadow @Final private Matrix3x2fStack pose;

  @Shadow @Final public GuiGraphicsExtractor.ScissorStack scissorStack;

  @Unique
  @SuppressWarnings("DataFlowIssue")
  private GuiGraphicsExtractor self() {
    return (GuiGraphicsExtractor) (Object) this;
  }

  // 1. 標準の textRenderer: 常に不透明 (1.0F)
  @Inject(
      method =
          "textRenderer(Lnet/minecraft/client/gui/GuiGraphicsExtractor$HoveredTextEffects;Ljava/util/function/Consumer;)Lnet/minecraft/client/gui/ActiveTextCollector;",
      at = @At("HEAD"),
      cancellable = true)
  public void onTextRenderer(
      GuiGraphicsExtractor.HoveredTextEffects hoveredTextEffects,
      @Nullable Consumer<Style> additionalHoverStyleConsumer,
      CallbackInfoReturnable<ActiveTextCollector> cir) {

    cir.setReturnValue(
        new ModernTextRenderer(self(), hoveredTextEffects, 1.0F, additionalHoverStyleConsumer));
  }

  // 2. ウィジェット用: ウィジェットの透過度を取得して渡す
  @Inject(method = "textRendererForWidget", at = @At("HEAD"), cancellable = true)
  public void onTextRendererForWidget(
      AbstractWidget owner,
      GuiGraphicsExtractor.HoveredTextEffects hoveredTextEffects,
      CallbackInfoReturnable<ActiveTextCollector> cir) {

    // ここでウィジェットの alpha を抽出
    float alpha = owner.getAlpha();

    cir.setReturnValue(new ModernTextRenderer(self(), hoveredTextEffects, alpha, null));
  }

  // 3. 直接描画用 (以前のロジックを維持)
  @Inject(
      method =
          "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
      at = @At("HEAD"),
      cancellable = true)
  public void onDrawString(
      Font font,
      FormattedCharSequence str,
      int x,
      int y,
      int color,
      boolean dropShadow,
      CallbackInfo ci) {
    if (ARGB.alpha(color) == 0) return;

    if (InfiniteClient.INSTANCE
        .getGlobalFeatures()
        .getRendering()
        .getInfiniteFontFeature()
        .isEnabled()) {
      IModernFontManager fontManager =
          (IModernFontManager) ((MinecraftAccessor) this.minecraft).getFontManager();
      Style originalStyle = extractStyle(str);
      FontSet fontSet = fontManager.infinite$fontSetFromStyle(originalStyle);
      Font modernFont = FontFromFontSetKt.fromFontSet(fontSet);

      FormattedCharSequence noBoldSequence = stripBold(str);

      this.guiRenderState.addText(
          new GuiTextRenderState(
              modernFont,
              noBoldSequence,
              new Matrix3x2f(this.pose),
              x,
              y,
              color,
              0,
              dropShadow,
              false,
              this.scissorStack.peek()));
      ci.cancel();
    }
  }

  @Unique
  private Style extractStyle(FormattedCharSequence sequence) {
    StyleExtractor extractor = new StyleExtractor();
    sequence.accept(extractor);
    return extractor.getFoundStyle();
  }

  @Unique
  private FormattedCharSequence stripBold(FormattedCharSequence original) {
    return (sink) ->
        original.accept(
            (index, style, codePoint) -> sink.accept(index, style.withBold(false), codePoint));
  }
}
