package org.infinite.mixin.infinite.features.global.rendering.theme.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.global.rendering.theme.ThemeFeature;
import org.infinite.libs.graphics.bundle.Graphics2DRenderer;
import org.infinite.libs.ui.theme.ColorScheme;
import org.infinite.libs.ui.theme.Theme;
import org.infinite.utils.ColorKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Button.Plain.class)
public abstract class PlainButtonMixin extends Button {
  protected PlainButtonMixin(
      int i,
      int j,
      int k,
      int l,
      Component component,
      OnPress onPress,
      CreateNarration createNarration) {
    super(i, j, k, l, component, onPress, createNarration);
  }

  @Inject(method = "renderContents", at = @At("HEAD"), cancellable = true)
  public void onRenderContent(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
    ThemeFeature themeFeature =
        InfiniteClient.INSTANCE.getGlobalFeatures().getRendering().getThemeFeature();
    if (themeFeature.isEnabled()) {
      Theme theme = InfiniteClient.INSTANCE.getTheme();
      ColorScheme colorScheme = theme.getColorScheme();
      Graphics2DRenderer graphics2DRenderer =
          new Graphics2DRenderer(guiGraphics, Minecraft.getInstance().getDeltaTracker());
      this.renderDefaultSprite(guiGraphics);
      // 3. テキストのスクロール描画
      String text = this.message.getString();
      int textWidth = Minecraft.getInstance().font.width(text);
      int innerWidth = getWidth() - 8; // 左右の余白

      graphics2DRenderer.getTextStyle().setFont("infinite_regular");
      graphics2DRenderer.getTextStyle().setShadow(true);

      int alphaInt = (int) (getAlpha() * 255);
      int color;
      if (isActive()) {
        color = ColorKt.alpha(colorScheme.getForegroundColor(), alphaInt);
      } else {
        color = ColorKt.alpha(colorScheme.getSecondaryColor(), alphaInt);
      }
      graphics2DRenderer.setFillStyle(color);

      if (textWidth > innerWidth && isHoveredOrFocused()) {
        // --- スクロール設定 ---
        int gap = 20; // テキスト間の空白
        double speed = 40.0; // 1秒間に移動するピクセル数
        double time = (double) System.currentTimeMillis() / 1000.0;

        // ループする周期（テキスト幅 + 空白分）
        float loopRange = textWidth + gap;
        float offset = (float) ((time * speed) % loopRange);

        guiGraphics.enableScissor(
            getX() + 4, getY(), getX() + getWidth() - 4, getY() + getHeight());

        graphics2DRenderer.getTextStyle().setShadow(false);

        // 1回目の描画（左へ流れる）
        graphics2DRenderer.text(text, getX() + 4 - offset, getY() + (float) getHeight() / 2);

        // 2回目の描画（1回目のすぐ後ろをついていく）
        // 1回目の末尾が見え始めたら、この2回目が右側から入ってくる
        graphics2DRenderer.text(
            text, getX() + 4 - offset + loopRange, getY() + (float) getHeight() / 2);

        guiGraphics.disableScissor();
      } else {
        // 通常時（中央揃え）
        graphics2DRenderer.textCentered(
            text, getX() + (float) getWidth() / 2, getY() + (float) getHeight() / 2);
      }

      graphics2DRenderer.flush();
      ci.cancel();
    }
  }
}
