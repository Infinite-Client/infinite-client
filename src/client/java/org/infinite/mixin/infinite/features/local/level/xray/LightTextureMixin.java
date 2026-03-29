package org.infinite.mixin.infinite.features.local.level.xray;

import net.minecraft.client.renderer.Lightmap;
import net.minecraft.world.level.dimension.DimensionType;
import org.infinite.InfiniteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Lightmap.class)
public class LightTextureMixin {

  @Inject(method = "getBrightness", at = @At("RETURN"), cancellable = true)
  private static void onGetBrightness(
      DimensionType dimensionType, int level, CallbackInfoReturnable<Float> cir) {
    if (InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature().isEnabled()) {
      float boostedValue = 15.0F;
      cir.setReturnValue(boostedValue);
    }
  }
}
