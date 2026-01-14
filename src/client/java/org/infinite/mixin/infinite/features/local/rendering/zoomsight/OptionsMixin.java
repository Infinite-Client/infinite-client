package org.infinite.mixin.infinite.features.local.rendering.zoomsight;

import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.infinite.InfiniteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {
  @Inject(method = "getCameraType", at = @At("RETURN"), cancellable = true)
  private void onGetCameraType(CallbackInfoReturnable<CameraType> cir) {
    if (InfiniteClient.INSTANCE
        .getLocalFeatures()
        .getRendering()
        .getZoomSightFeature()
        .isEnabled()) {
      cir.setReturnValue(CameraType.FIRST_PERSON);
    }
  }
}
