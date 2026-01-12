package org.infinite.mixin.infinite.features.local.rendering.brightsight;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.brightsight.BrightSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

  @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
  private static void onGetNightVisionScale(
      LivingEntity entity, float partialTick, CallbackInfoReturnable<Float> cir) {
    BrightSightFeature feature =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getBrightSightFeature();

    // 1. Featureが有効か確認
    if (feature.isEnabled()) {
      BrightSightFeature.Method method = feature.getMethod().getValue();

      // 2. UltraBright または NightSight の場合のみ 1.0f (最大) を返す
      if (method == BrightSightFeature.Method.UltraBright
          || method == BrightSightFeature.Method.NightSight) {
        cir.setReturnValue(1.0f);
      }
    }
  }
}
