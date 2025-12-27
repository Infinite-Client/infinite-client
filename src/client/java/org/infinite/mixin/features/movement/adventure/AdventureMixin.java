package org.infinite.mixin.features.movement.adventure;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.movement.adventure.Adventure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class AdventureMixin {

  // ブロック攻撃パケットをキャンセル (左クリック長押しなど)
  @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
  private void infinite$cancelAttackBlock(
      BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
    if (InfiniteClient.INSTANCE.isFeatureEnabled(Adventure.class)) {
      cir.setReturnValue(false); // 常にfalseを返し、ブロック破壊の進行を停止させる
      cir.cancel(); // オリジナルメソッドの実行をキャンセル
    }
  }

  // ブロック破壊進行パケットをキャンセル (ブロックを叩くアニメーションなど)
  @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
  private void infinite$cancelUpdateBlockBreakingProgress(
      BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
    if (InfiniteClient.INSTANCE.isFeatureEnabled(Adventure.class)) {
      cir.cancel(); // オリジナルメソッドの実行をキャンセル
    }
  }
}
