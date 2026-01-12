package org.infinite.mixin.infinite.features.local.rendering.brightsight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.client.renderer.LightTexture;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.brightsight.BrightSightFeature;
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
              remap = false // ライブラリ側のメソッドなので remap は false
              ))
  private Std140Builder onPutFloat(
      Std140Builder instance, float value, Operation<Std140Builder> original) {
    BrightSightFeature feature =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getBrightSightFeature();

    // 1. Featureが有効か確認
    if (feature.isEnabled()) {
      BrightSightFeature.Method method = feature.getMethod().getValue();

      // 2. GamMax または UltraBright の場合、輝度を最大 (15.0F) に固定
      // これにより、ブロックの明るさ(Block Light)と空の明るさ(Sky Light)の両方が無視され、全画面が明るくなります
      if (method == BrightSightFeature.Method.GamMax
          || method == BrightSightFeature.Method.UltraBright) {
        return original.call(instance, 15.0F);
      }
    }

    return original.call(instance, value);
  }
}
