package org.infinite.mixin.infinite.features.local.rendering.brightsight;

import net.minecraft.client.renderer.Lightmap;
import net.minecraft.world.level.dimension.DimensionType;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.brightsight.BrightSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Lightmap.class)
public class LightmapMixin {

  @Inject(method = "getBrightness", at = @At("RETURN"), cancellable = true)
  private static void onPutFloat(
      DimensionType dimensionType, int level, CallbackInfoReturnable<Float> cir) {
    BrightSightFeature feature =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getBrightSightFeature();
    if (feature.isEnabled()) {
      BrightSightFeature.Method method = feature.getMethod().getValue();
      if (method == BrightSightFeature.Method.GamMax
          || method == BrightSightFeature.Method.UltraBright) {
        cir.setReturnValue(1f);
      }
    }
  }
}
