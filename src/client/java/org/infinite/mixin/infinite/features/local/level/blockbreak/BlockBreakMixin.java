package org.infinite.mixin.infinite.features.local.level.blockbreak;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.level.blockbreak.FastBreakFeature;
import org.infinite.infinite.features.local.level.blockbreak.LinearBreakFeature;
import org.infinite.infinite.features.local.level.blockbreak.VeinBreakFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class BlockBreakMixin {

  @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
  private void onStartDestroyBlock(
      BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
    VeinBreakFeature veinBreak =
        InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getVeinBreakFeature();
    LinearBreakFeature linearBreak =
        InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getLinearBreakFeature();

    // 1. VeinBreakを最優先 (鉱石ならこちらが引き受ける)
    if (veinBreak.isEnabled() && veinBreak.tryAdd(pos)) {
      cir.setReturnValue(false);
      cir.cancel();
      return;
    }
    // 2. VeinBreakが対象外なら LinearBreak が引き受ける
    if (linearBreak.isEnabled() && linearBreak.tryAdd(pos)) {
      cir.setReturnValue(false);
      cir.cancel();
    }
  }

  @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
  private void onContinueDestroyBlock(
      BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
    if (InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getVeinBreakFeature().isWorking()
        || InfiniteClient.INSTANCE
            .getLocalFeatures()
            .getLevel()
            .getLinearBreakFeature()
            .isWorking()) {
      cir.setReturnValue(false);
      cir.cancel();
      return;
    }

    FastBreakFeature fastBreak =
        InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getFastBreakFeature();
    if (fastBreak.isEnabled()) {
      MultiPlayerGameModeAccessor accessor = (MultiPlayerGameModeAccessor) this;
      float progress = accessor.getDestroyProgress();
      if (fastBreak.shouldFastBreak(pos, progress)) {
        fastBreak.sendStopPacket(pos, direction);
        accessor.setIsDestroying(false);
        accessor.setDestroyProgress(0f);
        cir.setReturnValue(true);
        cir.cancel();
      }
    }
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void onTick(CallbackInfo ci) {
    FastBreakFeature fastBreak =
        InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getFastBreakFeature();
    if (fastBreak.isEnabled()) {
      ((MultiPlayerGameModeAccessor) this).setDestroyDelay(0);
    }
  }
}
