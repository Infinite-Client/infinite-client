package org.infinite.mixin.infinite.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import org.infinite.InfiniteClient;
import org.infinite.global.rendering.theme.ThemeSetting;
import org.infinite.global.rendering.theme.widget.ScrollbarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractScrollArea.class)
public abstract class AbstractScrollAreaMixin {

  @Inject(method = "renderScrollbar", at = @At("HEAD"), cancellable = true)
  protected void infiniteClient$drawScrollbar(
      GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
    ThemeSetting themeSetting = InfiniteClient.INSTANCE.getGlobalFeature(ThemeSetting.class);
    if (themeSetting != null && themeSetting.isEnabled()) {
      ScrollbarRenderer renderer = new ScrollbarRenderer((AbstractScrollArea) (Object) this);
      renderer.renderScrollbar(
          context, mouseX, mouseY, 0.0f); // delta is not used in original drawScrollbar
      ci.cancel();
    }
  }
}
