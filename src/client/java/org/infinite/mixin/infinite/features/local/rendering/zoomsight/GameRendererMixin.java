package org.infinite.mixin.infinite.features.local.rendering.zoomsight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.zoomsight.ZoomSightFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
  @Shadow private float fovModifier;

  @Shadow private float oldFovModifier;

  @Shadow @Final private Minecraft minecraft;

  @Inject(method = "tickFov", at = @At("HEAD"), cancellable = true)
  private void onTickFov(CallbackInfo ci) {
    ZoomSightFeature zoomSightFeature =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getZoomSightFeature();
    if (zoomSightFeature.isEnabled()) {
      Entity entity = this.minecraft.getCameraEntity();
      float factor;
      if (entity instanceof AbstractClientPlayer abstractClientPlayer) {
        Options options = this.minecraft.options;
        boolean bl = options.getCameraType().isFirstPerson();
        float f = options.fovEffectScale().get().floatValue();
        factor = abstractClientPlayer.getFieldOfViewModifier(bl, f);
      } else {
        factor = 1.0F;
      }

      this.oldFovModifier = this.fovModifier;
      this.fovModifier += (factor - this.fovModifier) * 0.5F;
      ci.cancel();
    }
  }
}
