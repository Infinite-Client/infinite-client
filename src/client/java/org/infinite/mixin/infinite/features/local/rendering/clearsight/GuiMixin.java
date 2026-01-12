package org.infinite.mixin.infinite.features.local.rendering.clearsight;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
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
    if (feature.isEnabled() && feature.getAntiOverlay().getValue()) {
      ci.cancel();
    }
  }
}
