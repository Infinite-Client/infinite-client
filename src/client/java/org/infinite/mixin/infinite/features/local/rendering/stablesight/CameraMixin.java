package org.infinite.mixin.infinite.features.local.rendering.stablesight;

import net.minecraft.client.Camera;
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

  @Shadow private boolean detached;

  @Unique
  private StableSightFeature stableSightFeature() {
    return InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getStableSightFeature();
  }

  @Inject(method = "setup", at = @At("RETURN"))
  public void onSetupReturn(
      net.minecraft.world.level.BlockGetter blockGetter,
      net.minecraft.world.entity.Entity entity,
      boolean bl,
      boolean bl2,
      float f,
      CallbackInfo ci) {
    if (stableSightFeature().isEnabled() && this.detached) {
      float customDistance = stableSightFeature().getCameraDistance().getValue();
      boolean ignoreTerrain = stableSightFeature().getIgnoreTerrain().getValue();

      if (ignoreTerrain) {
        // setupの中で既に実行されたmoveを打ち消すために、一旦リセットしてから再移動
        // (注意: 実際にはsetupのロジックに合わせて微調整が必要)
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
