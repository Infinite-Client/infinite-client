package org.infinite.mixin.infinite.features.local.rendering.xray;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.infinite.InfiniteClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {
  @Shadow @Final private OptionInstance<Double> gamma;

  @Unique private OptionInstance<Double> infinite$maxGamma;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void initMaxGamma(CallbackInfo ci) {
    // 常に 1.0 を保持し、変更されても何もせず、値も保存されないインスタンス
    this.infinite$maxGamma =
        new OptionInstance<>(
            "options.gamma",
            OptionInstance.noTooltip(),
            (caption, value) -> caption, // 表示名の整形（不要ならデフォルト）
            OptionInstance.UnitDouble.INSTANCE, // 0.0-1.0の範囲を扱う定義
            1.0, // デフォルト値
            (v) -> {} // 値がセットされた時のコールバックを空にする（変更不可にする）
            );
  }

  @Inject(method = "gamma", at = @At("HEAD"), cancellable = true)
  private void onGetGamma(CallbackInfoReturnable<OptionInstance<Double>> cir) {
    // XRayが有効な場合のみ、最大値固定のインスタンスを返す
    if (InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature().isEnabled()) {
      cir.setReturnValue(this.infinite$maxGamma);
    }
  }
}
