package org.infinite.mixin.features.rendering.detailinfo;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.rendering.detailinfo.DetailInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// ターゲットクラスの直接のスーパークラス（LootableContainerBlockEntity）を継承させるか、継承を外す
// 元のコードの記述に従い、抽象クラスとして定義します。
@Mixin(ChestBlockEntity.class)
public abstract class ChestMixin {
  @Unique
  private static boolean shouldCancel() {
    // 設定チェックのロジックを統合
    DetailInfo detailInfo = InfiniteClient.INSTANCE.getFeature(DetailInfo.class);
    return detailInfo != null
        && InfiniteClient.INSTANCE.isSettingEnabled(DetailInfo.class, "InnerChest")
        && detailInfo.getShouldCancelScanScreen();
  }

  @Unique private static Boolean cancelFlag = false;

  // アニメーションの停止 (getAnimationProgress)
  @Inject(method = "getOpenNess", at = @At("RETURN"), cancellable = true)
  private void infiniteClient$forceZeroChestAnimation(
      float tickProgress, CallbackInfoReturnable<Float> cir) {
    if (shouldCancel()) {
      cancelFlag = true;
    }
    if (cancelFlag) {
      if (cir.getReturnValue() == 1.0f) {
        cancelFlag = false;
      }
      cir.setReturnValue(0.0F);
    }
  }

  // サウンドの停止 (playSound - static)
  @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
  private static void infiniteClient$cancelChestSound(
      Level world, BlockPos pos, BlockState state, SoundEvent soundEvent, CallbackInfo ci) {
    if (cancelFlag) {
      ci.cancel();
    }
  }
}
