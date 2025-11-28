package org.infinite.mixin.infinite.gui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
  private static final Identifier INFINITE_ICON = Identifier.of("infinite", "icon.png");

  @Redirect(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V"))
  private void infiniteClient$replacePanorama(
      TitleScreen instance, DrawContext context, float delta) {
    int width = context.getScaledWindowWidth();
    int height = context.getScaledWindowHeight();

    int top = ColorHelper.getArgb(255, 8, 10, 14);
    int bottom = ColorHelper.getArgb(255, 0, 0, 0);
    context.fillGradient(0, 0, width, height, top, bottom);

    int size = Math.min(width, height) / 3;
    size = Math.max(96, Math.min(size, 180));
    int x = (width - size) / 2;
    int y = Math.max(height / 5, (height - size) / 2);
    int logoColor = ColorHelper.getArgb(80, 255, 255, 255);

    context.drawTexture(
        RenderPipelines.GUI_TEXTURED,
        INFINITE_ICON,
        x,
        y,
        0f,
        0f,
        size,
        size,
        256,
        256,
        256,
        256,
        logoColor);
  }
}
