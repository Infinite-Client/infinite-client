package org.infinite.mixin.infinite.features.local.rendering.zoomsight;

import net.minecraft.client.player.AbstractClientPlayer;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.zoomsight.ZoomSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractClientPlayer.class, priority = 900)
public class AbstractClientPlayerMixin {

  // 視覚的な現在のズーム倍率を保持（1.0 = ズームなし）
  @Unique private float visualZoom = 1.0f;

  @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
  private void onGetFieldOfViewModifier(CallbackInfoReturnable<Float> cir) {
    float fov = cir.getReturnValue();
    // --- 優先度1: ZoomSight (ズームを適用) ---
    ZoomSightFeature zoom =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getZoomSightFeature();
    if (zoom.isEnabled()) {
      float targetZoom = zoom.getCurrentZoom();
      if (zoom.getSmoothZoom().getValue()) {
        visualZoom += (targetZoom - visualZoom) * 0.25f;
      } else {
        visualZoom = targetZoom;
      }

      if (visualZoom > 1.0f) {
        fov /= visualZoom; // 視野を狭めてズーム
      }
      cir.setReturnValue(fov);
      cir.cancel();
    } else {
      visualZoom = 1.0f;
    }
  }
}
