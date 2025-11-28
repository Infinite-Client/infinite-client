package org.infinite.mixin.infinite.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

  /**
   * Replace vanilla panorama background with a simple dark gradient for all menu screens
   * (singleplayer, multiplayer, etc.). TitleScreen has its own mixin, so this covers the rest.
   */
  @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
  private void infiniteClient$renderBlackBackground(
      DrawContext context, float delta, CallbackInfo ci) {
    int width = context.getScaledWindowWidth();
    int height = context.getScaledWindowHeight();
    int top = ColorHelper.getArgb(255, 8, 10, 14);
    int bottom = ColorHelper.getArgb(255, 0, 0, 0);
    context.fillGradient(0, 0, width, height, top, bottom);
    ci.cancel();
  }
}
