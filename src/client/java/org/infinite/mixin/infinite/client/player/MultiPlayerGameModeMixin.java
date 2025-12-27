package org.infinite.mixin.infinite.client.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.infinite.libs.client.player.PlayerStatsManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

  @Inject(method = "attack", at = @At("HEAD"))
  private void onClientAttack(Player player, Entity target, CallbackInfo ci) {
    PlayerStatsManager.INSTANCE.handleEntityAttack();
  }

  @Shadow @Final private Minecraft minecraft;

  @Inject(method = "destroyBlock", at = @At("RETURN"))
  private void onClientBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValue() && this.minecraft.player != null) {
      PlayerStatsManager.INSTANCE.handleBlockBreak();
    }
  }
}
