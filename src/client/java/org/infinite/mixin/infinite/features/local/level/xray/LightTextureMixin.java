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

  /**
   * updateLightTexture (method_3313) 内の putFloat をラップします。 remap = true (デフォルト)
   * にすることで本番環境の難読化に対応させます。
   */
  @WrapOperation(
      method = "updateLightTexture",
      at =
          @At(
              value = "INVOKE",
              // targetはマッピングされるため、remap = falseを外す
              target =
                  "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;"))
  private Std140Builder onPutFloat(
      Std140Builder instance, float value, Operation<Std140Builder> original) {
    // XRayが無効なら何もしない
    if (!InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature().isEnabled()) {
      return original.call(instance, value);
    }

    // XRay有効時は、明るさの値を最大（15.0F相当の処理）にブースト
    // ※値は環境により調整が必要ですが、基本は元の値を無視して高い値を渡します
    float boostedValue = 15.0F;
    return original.call(instance, boostedValue);
  }
}
