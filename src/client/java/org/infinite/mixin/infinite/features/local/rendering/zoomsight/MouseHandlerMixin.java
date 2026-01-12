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
  private void onMouseScroll(long handle, double xOffset, double yOffset, CallbackInfo ci) {
    ZoomSightFeature zoom =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getZoomSightFeature();

    if (zoom.isEnabled()) {
      float current = zoom.getCurrentZoom();
      float step = zoom.getZoomStep().getValue(); // プロパティから取得

      float next;
      if (yOffset > 0) {
        next = current * step; // 拡大
      } else {
        next = current / step; // 縮小
      }

      zoom.setCurrentZoom(Math.max(1.0f, Math.min(50.0f, next)));
      ci.cancel();
    }
  }

  /** ズーム倍率に応じた感度の動的補正 変数 'f' (e * e * e) は Minecraft 1.21 における回転デルタの基数です。 */
  @ModifyVariable(method = "turnPlayer", at = @At("STORE"), ordinal = 1)
  private double modifySensitivityMultiplier(double originalF) {
    ZoomSightFeature zoom =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getZoomSightFeature();

    if (zoom.isEnabled()) {
      float level = zoom.getCurrentZoom();
      float reduction = zoom.getSensitivityReduction().getValue();

      if (level > 1.0f) {
        // 感度を (1 / 倍率) に近づける補正
        // reduction = 1.0 の時、倍率に完全反比例します
        return originalF / (1.0 + (level - 1.0) * reduction);
      }
    }
    return originalF;
  }
}
