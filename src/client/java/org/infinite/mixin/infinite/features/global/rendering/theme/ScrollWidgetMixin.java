package org.infinite.mixin.infinite.features.global.rendering.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.global.rendering.theme.ThemeFeature;
import org.infinite.libs.graphics.bundle.Graphics2DRenderer;
import org.infinite.libs.ui.theme.ColorScheme;
import org.infinite.libs.ui.theme.Theme;
import org.infinite.utils.ColorKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractScrollArea.class)
public abstract class ScrollWidgetMixin extends AbstractWidget {
  @Shadow private double scrollAmount;

  @Shadow
  public abstract boolean scrollbarVisible();

  @Shadow
  public abstract int scrollBarX();

  @Shadow
  public abstract int scrollerHeight();

  @Shadow
  public abstract int scrollBarY();

  @Shadow
  public abstract boolean isOverScrollbar(double d, double e);

  public ScrollWidgetMixin(int i, int j, int k, int l, Component component) {
    super(i, j, k, l, component);
  }

  @Inject(method = "renderScrollbar", at = @At("HEAD"), cancellable = true)
  private void onRenderScrollBar(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
    ThemeFeature themeFeature =
        InfiniteClient.INSTANCE.getGlobalFeatures().getRendering().getThemeFeature();
    if (!themeFeature.isEnabled()) return;

    if (this.scrollbarVisible()) {
      Theme theme = InfiniteClient.INSTANCE.getTheme();
      ColorScheme colorScheme = theme.getColorScheme();
      Graphics2DRenderer graphics2DRenderer =
          new Graphics2DRenderer(guiGraphics, Minecraft.getInstance().getDeltaTracker());

      int x = this.scrollBarX();
      int barHeight = this.scrollerHeight();
      int barY = this.scrollBarY();
      int barWidth = 4; // バニラは6pxですが、モダンにするため少し細めの4pxに調整（お好みで）
      int xCentered = x + 1; // 6px幅の中央に配置

      theme.renderBackGround(x, barY, barWidth, barHeight, graphics2DRenderer, 0.5f);

      // 2. スクロールバー本体（つまみ）
      int barColor = colorScheme.getAccentColor();

      // ホバー中、またはドラッグ中（scrolling）は色を強調する
      if (this.isOverScrollbar(i, j) || this.isHoveredOrFocused()) {
        barColor = ColorKt.alpha(barColor, 255);
      } else {
        barColor = ColorKt.alpha(barColor, 160); // 通常時は少し透かす
      }

      graphics2DRenderer.setFillStyle(barColor);
      graphics2DRenderer.fillRect(xCentered, barY, barWidth, barHeight);

      graphics2DRenderer.flush();

      this.isOverScrollbar(i, j);

      ci.cancel();
    }
  }
}
