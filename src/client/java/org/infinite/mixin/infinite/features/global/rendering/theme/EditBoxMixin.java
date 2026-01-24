package org.infinite.mixin.infinite.features.global.rendering.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.global.rendering.theme.ThemeFeature;
import org.infinite.libs.graphics.bundle.Graphics2DRenderer;
import org.infinite.libs.ui.theme.ColorScheme;
import org.infinite.libs.ui.theme.Theme;
import org.infinite.utils.ColorKt;
import org.infinite.utils.FontKt;
import org.infinite.utils.WidgetRenderUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends net.minecraft.client.gui.components.AbstractWidget {

  @Shadow private String value;
  @Shadow public int displayPos; // 表示開始インデックス
  @Shadow public int cursorPos;
  @Shadow public int highlightPos;

  @Unique @Final private final Minecraft minecraft = Minecraft.getInstance();

  @Shadow
  public abstract int getInnerWidth();

  public EditBoxMixin(int i, int j, int k, int l, Component component) {
    super(i, j, k, l, component);
  }

  @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
  protected void onRenderWidget(
      GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
    if (minecraft.screen instanceof ChatScreen) {
      return; // 早期リターンしてバニラのレンダリングを行わせる
    }
    ThemeFeature themeFeature =
        InfiniteClient.INSTANCE.getGlobalFeatures().getRendering().getThemeFeature();
    if (!themeFeature.isEnabled()) return;
    String fontName = "infinite_regular";
    Theme theme = InfiniteClient.INSTANCE.getTheme();
    ColorScheme colorScheme = theme.getColorScheme();
    Graphics2DRenderer renderer = new Graphics2DRenderer(guiGraphics, minecraft.getDeltaTracker());
    Font font = FontKt.Font(fontName);

    WidgetRenderUtils.INSTANCE.renderCustomBackground(this, renderer);
    // 2. テキスト描画の準備
    int textColor = ColorKt.alpha(colorScheme.getForegroundColor(), (int) (getAlpha() * 255));
    int textX = getX() + 4;
    int textY = getY() + (getHeight() - 8) / 2;
    int innerWidth = getInnerWidth();

    // 表示する範囲の文字列を切り出す
    String visibleText = font.plainSubstrByWidth(this.value.substring(this.displayPos), innerWidth);
    int cursorOffset = this.cursorPos - this.displayPos;
    int highlightOffset = this.highlightPos - this.displayPos;

    // 3. テキスト本体の描画
    renderer.getTextStyle().setFont(fontName);
    renderer.getTextStyle().setShadow(false);
    renderer.setFillStyle(textColor);

    renderer.enableScissor(getX() + 2, getY() + 2, getWidth() - 2, getHeight() - 2);

    renderer.text(visibleText, textX, textY);

    // 4. カーソルの描画
    boolean showCursor = isFocused() && (System.currentTimeMillis() / 500) % 2 == 0;
    int cursorX;

    if (cursorOffset >= 0 && cursorOffset <= visibleText.length()) {
      cursorX = textX + font.width(visibleText.substring(0, cursorOffset));
      if (showCursor) {
        renderer.setFillStyle(colorScheme.getAccentColor());
        renderer.fillRect(cursorX, textY, 1, 9); // 1px幅のカーソル
      }
    }

    // 5. 選択範囲（ハイライト）の描画
    if (highlightOffset != cursorOffset) {
      int startX =
          textX
              + font.width(
                  visibleText.substring(
                      0,
                      Math.max(
                          0,
                          Math.min(
                              visibleText.length(), Math.min(cursorOffset, highlightOffset)))));
      int endX =
          textX
              + font.width(
                  visibleText.substring(
                      0,
                      Math.max(
                          0,
                          Math.min(
                              visibleText.length(), Math.max(cursorOffset, highlightOffset)))));

      renderer.setFillStyle(ColorKt.alpha(colorScheme.getAccentColor(), 100));
      renderer.fillRect(startX, textY, endX - startX, 9);
    }

    renderer.disableScissor();
    renderer.flush();

    ci.cancel();
  }
}
