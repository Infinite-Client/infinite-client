package org.infinite.mixin.infinite.features.local.level.xray;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.client.renderer.LightTexture;
import org.infinite.InfiniteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public class LightTextureMixin {

  @WrapOperation(
      method = "updateLightTexture",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
              remap = false))
  private Std140Builder onPutFloat(
      Std140Builder instance, float value, Operation<Std140Builder> original) {
    if (!InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature().isEnabled()) {
      return original.call(instance, value);
    }
    float boostedValue = 15.0F;
    return original.call(instance, boostedValue);
  }
}
