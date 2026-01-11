package org.infinite.mixin.infinite.features.global.rendering.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.global.rendering.theme.ThemeFeature;
import org.infinite.libs.graphics.bundle.Graphics2DRenderer;
import org.infinite.libs.ui.theme.ColorScheme;
import org.infinite.libs.ui.theme.Theme;
import org.infinite.utils.ColorKt;
import org.infinite.utils.WidgetRenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class SliderButtonMixin extends net.minecraft.client.gui.components.AbstractWidget {

  @Shadow public double value;

  public SliderButtonMixin(int i, int j, int k, int l, Component component) {
    super(i, j, k, l, component);
  }

  @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
  protected void onRenderWidget(
      GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
    ThemeFeature themeFeature =
        InfiniteClient.INSTANCE.getGlobalFeatures().getRendering().getThemeFeature();
    if (!themeFeature.isEnabled()) return;

    Theme theme = InfiniteClient.INSTANCE.getTheme();
    ColorScheme colorScheme = theme.getColorScheme();
    Graphics2DRenderer renderer =
        new Graphics2DRenderer(guiGraphics, Minecraft.getInstance().getDeltaTracker());

    WidgetRenderUtils.INSTANCE.renderCustomBackground(this, renderer);
    // 2. ハンドル（ノブ）の描画
    // ハンドルの位置を計算 (value は 0.0 ~ 1.0)
    int handleWidth = 8;
    float handleX = (float) (getX() + (value * (getWidth() - handleWidth)));

    // ハンドルの色
    int handleColor =
        isHoveredOrFocused() ? colorScheme.getAccentColor() : colorScheme.getSecondaryColor();
    renderer.setFillStyle(ColorKt.alpha(handleColor, (int) (getAlpha() * 255)));

    // ハンドルを少しだけ縦長、あるいはボタンの高さに合わせる
    renderer.fillRect(handleX, getY(), handleWidth, getHeight());

    // 3. テキストの描画
    renderer.getTextStyle().setFont("infinite_regular");
    renderer.getTextStyle().setShadow(false);
    renderer.setFillStyle(
        ColorKt.alpha(colorScheme.getForegroundColor(), (int) (getAlpha() * 255)));
    renderer.textCentered(
        getMessage().getString(),
        getX() + (float) getWidth() / 2,
        getY() + (float) getHeight() / 2);

    renderer.flush();
    ci.cancel();
  }
}
