package org.infinite.mixin.infinite.features.local.rendering.clearsight;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.clearsight.ClearSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

  @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
  private void onRenderTextureOverlay(
      GuiGraphics guiGraphics, Identifier identifier, float f, CallbackInfo ci) {
    ClearSightFeature feature =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getClearSightFeature();
    if (!feature.isEnabled() || !feature.getAntiOverlay().getValue()) return;

    String path = identifier.getPath();

    // 粉雪のオーバーレイ (powder_snow_outline)
    if (path.contains("powder_snow_outline")) {
      guiGraphics.pose().pushMatrix();
      float scale = 1.2f;
      int w = guiGraphics.guiWidth();
      int h = guiGraphics.guiHeight();
      guiGraphics.pose().translate(w / 2f, h / 2f);
      guiGraphics.pose().scale(scale, scale);
      guiGraphics.pose().translate(-w / 2f, -h / 2f);
      int color = 0x44FFFFFF;
      guiGraphics.blit(
          RenderPipelines.GUI_TEXTURED,
          identifier,
          0,
          0,
          0.0F,
          0.0F,
          guiGraphics.guiWidth(),
          guiGraphics.guiHeight(),
          guiGraphics.guiWidth(),
          guiGraphics.guiHeight(),
          color);
      guiGraphics.pose().popMatrix();
      ci.cancel();
    }
  }
}
