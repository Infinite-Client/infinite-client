package org.infinite.mixin.infinite.features.local.rendering.stablesight;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.stablesight.StableSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

  @Shadow
  protected abstract void move(float f, float g, float h);

  @Shadow private float xRot;
  @Shadow private float yRot;
  @Shadow private boolean detached;

  @Unique
  private StableSightFeature stableSightFeature() {
    return InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getStableSightFeature();
  }

  /** setupの最後で実行されるmove(カメラの後退処理)を上書き、 またはgetMaxZoomの挙動を書き換えます。 */
  @Inject(method = "setup", at = @At("RETURN"))
  public void onSetupReturn(
      Level level, Entity entity, boolean bl, boolean bl2, float f, CallbackInfo ci) {
    if (stableSightFeature().isEnabled() && this.detached) {
      // 1. 標準の挙動で移動してしまった分を一旦リセット（moveは相対移動のため）
      // 既存のsetup内で getMaxZoom に基づいて move(-dist, 0, 0) が呼ばれている。
      // 地形を無視したい場合は、一旦位置を戻すか、計算自体を書き換える必要があります。

      // 2. 独自の距離と地形無視設定を適用
      float customDistance = stableSightFeature().getCameraDistance().getValue(); // Featureから取得
      boolean ignoreTerrain = stableSightFeature().getIgnoreTerrain().getValue();

      if (ignoreTerrain) {
        // getMaxZoomを通さずに、直接指定距離分下がる
        // setupの最後で再度呼び出すことで上書き
        this.move(-customDistance, 0.0F, 0.0F);
      }
    }
  }

  /** getMaxZoom メソッド自体を Hook して、地形無視が有効な場合は 入力された距離をそのまま返す（衝突判定をスキップする） */
  @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
  private void onGetMaxZoom(float f, CallbackInfoReturnable<Float> cir) {
    if (stableSightFeature().isEnabled() && stableSightFeature().getIgnoreTerrain().getValue()) {
      // 地形判定を行わず、要求された距離(f)をそのまま返す
      cir.setReturnValue(f);
    }
  }
}
