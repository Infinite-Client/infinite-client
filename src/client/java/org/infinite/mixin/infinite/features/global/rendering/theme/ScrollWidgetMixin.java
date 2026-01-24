// ScrollWidgetMixin.java
package org.infinite.mixin.infinite.features.global.rendering.theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.global.rendering.theme.ThemeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractScrollArea.class)
public abstract class ScrollWidgetMixin extends AbstractWidget {

  public ScrollWidgetMixin(int i, int j, int k, int l, Component component) {
    super(i, j, k, l, component);
  }

  @Inject(method = "renderScrollbar", at = @At("HEAD"), cancellable = true)
  private void onRenderScrollBar(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
    ThemeFeature theme =
        InfiniteClient.INSTANCE.getGlobalFeatures().getRendering().getThemeFeature();

    if (theme.shouldRenderCustom()) {
      // AbstractScrollArea 用の専用メソッドを呼び出す
      theme
          .getScrollWidgetRenderer()
          .renderScrollbar((AbstractScrollArea) (Object) this, guiGraphics, mouseX, mouseY);
      ci.cancel();
    }
  }
}
