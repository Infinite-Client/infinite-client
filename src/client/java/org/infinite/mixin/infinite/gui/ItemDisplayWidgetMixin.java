package org.infinite.mixin.infinite.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ItemDisplayWidget;
import org.infinite.InfiniteClient;
import org.infinite.libs.global.rendering.theme.ThemeSetting;
import org.infinite.libs.global.rendering.theme.widget.ItemStackWidgetRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemDisplayWidget.class)
public class ItemDisplayWidgetMixin {

  @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
  private void infiniteClient$renderWidget(
      GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    ThemeSetting themeSetting = InfiniteClient.INSTANCE.getGlobalFeature(ThemeSetting.class);
    if (themeSetting != null && themeSetting.isEnabled()) {
      ItemStackWidgetRenderer renderer =
          new ItemStackWidgetRenderer((ItemDisplayWidget) (Object) this);
      renderer.renderWidget(context, mouseX, mouseY, delta);
      ci.cancel();
    }
  }
}
