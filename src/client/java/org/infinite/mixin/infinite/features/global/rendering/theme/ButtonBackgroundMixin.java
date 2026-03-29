package org.infinite.mixin.infinite.features.global.rendering.theme;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.global.rendering.theme.ThemeFeature;
import org.infinite.infinite.features.global.rendering.theme.renderer.WidgetRenderUtils;
import org.infinite.libs.graphics.bundle.Graphics2DRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class ButtonBackgroundMixin extends AbstractWidget.WithInactiveMessage {

  public ButtonBackgroundMixin(
      int i, int j, int k, int l, net.minecraft.network.chat.Component component) {
    super(i, j, k, l, component);
  }

  @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
  private void onExtractDefaultSprite(GuiGraphicsExtractor graphics, CallbackInfo ci) {
    ThemeFeature themeFeature =
        InfiniteClient.INSTANCE.getGlobalFeatures().getRendering().getThemeFeature();
    if (!themeFeature.isEnabled()) return;

    Graphics2DRenderer renderer = new Graphics2DRenderer(graphics);

    WidgetRenderUtils.INSTANCE.renderCustomBackground(this, renderer);

    renderer.flush();
    ci.cancel();
  }
}
