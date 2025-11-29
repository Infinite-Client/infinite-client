package org.infinite.mixin.infinite.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.infinite.InfiniteClient;
import org.infinite.global.rendering.title.TitleScreenRenderer;
import org.infinite.global.rendering.title.TitleScreenSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
  @WrapOperation(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V"))
  private void infiniteClient$replacePanorama(
      TitleScreen instance, DrawContext drawContext, float v, Operation<Void> original) {
    if (InfiniteClient.INSTANCE.isGlobalFeatureEnabled(TitleScreenSetting.class)) {
      TitleScreenRenderer titleScreenRenderer = new TitleScreenRenderer(instance);
      titleScreenRenderer.render(drawContext);
    } else {
      original.call(instance, drawContext, v);
    }
  }
}
