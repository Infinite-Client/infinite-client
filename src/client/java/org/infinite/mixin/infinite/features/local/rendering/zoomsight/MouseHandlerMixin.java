package org.infinite.mixin.infinite.features.local.rendering.zoomsight;

import net.minecraft.client.MouseHandler;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.zoomsight.ZoomSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

  /** マウスホイールによる倍率調整 */
  @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
  private void onMouseScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
    ZoomSightFeature zoom =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getZoomSightFeature();

    if (zoom.isEnabled()) {
      float current = zoom.getCurrentZoom();
      float step = zoom.getZoomStep().getValue(); // プロパティから取得

      float next;
      if (yoffset > 0) {
        next = current * step; // 拡大
      } else {
        next = current / step; // 縮小
      }

      zoom.setCurrentZoom(Math.clamp(next, 1.0f, 50.0f));
      ci.cancel();
    }
  }

  @ModifyVariable(method = "turnPlayer", at = @At("STORE"), ordinal = 1)
  private double modifySensitivityMultiplier(double ss) {
    ZoomSightFeature zoom =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getZoomSightFeature();

    if (zoom.isEnabled()) {
      float level = zoom.getCurrentZoom();
      float reduction = zoom.getSensitivityReduction().getValue();

      if (level > 1.0f) {
        // 三乗根（Cube Root）をとることで、高倍率でも操作性を失わない
        // 50倍ズーム時：1/50 ≒ 0.02 (重すぎ) -> 1/7.07 (平方根) -> 1/3.68 (三乗根)
        double visualSpeedMatch = 1.0 / Math.cbrt(level);

        // reduction = 1.0 のとき、三乗根による補正がフルに効く
        double factor = 1.0 + (visualSpeedMatch - 1.0) * reduction;

        return ss * factor;
      }
    }
    return ss;
  }
}
