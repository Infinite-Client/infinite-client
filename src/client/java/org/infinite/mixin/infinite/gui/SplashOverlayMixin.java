package org.infinite.mixin.infinite.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.infinite.global.rendering.theme.overlay.InfiniteLoadingScreenRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
  @Shadow private float progress;
  @Shadow private long reloadCompleteTime;
  @Shadow private long reloadStartTime;
  @Shadow private boolean reloading;

  @Inject(method = "render", at = @At("TAIL"))
  private void infiniteClient$overlay(
      DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    // Draw our overlay after vanilla so we fully cover Mojang while still letting progress update.
    InfiniteLoadingScreenRenderer.render(
        context, this.progress, this.reloadStartTime, this.reloadCompleteTime, this.reloading);
  }

  // Suppress vanilla splash drawing while keeping its logic (progress, completion) intact.
  @Redirect(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
  private void infiniteClient$skipVanillaFill(
      DrawContext instance, int x1, int y1, int x2, int y2, int color) {
    // no-op to hide Mojang background
  }

  @Redirect(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"))
  private void infiniteClient$skipVanillaTexture(
      DrawContext instance,
      RenderPipeline pipeline,
      net.minecraft.util.Identifier id,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      int color) {
    // no-op to hide Mojang logos
  }

  @Redirect(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/screen/SplashOverlay;renderProgressBar(Lnet/minecraft/client/gui/DrawContext;IIIIF)V"))
  private void infiniteClient$skipVanillaProgressBar(
      SplashOverlay self,
      DrawContext context,
      int minX,
      int minY,
      int maxX,
      int maxY,
      float opacity) {
    // no-op to hide Mojang progress bar
  }

  @Redirect(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorTexture(Lcom/mojang/blaze3d/textures/GpuTexture;I)V"))
  private void infiniteClient$skipVanillaClear(CommandEncoder encoder, GpuTexture texture, int argb) {
    // no-op to avoid the brand red clear
  }
}
