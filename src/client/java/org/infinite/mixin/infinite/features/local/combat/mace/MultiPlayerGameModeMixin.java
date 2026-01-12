package org.infinite.mixin.infinite.features.local.combat.mace;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.combat.mace.MaceBoostFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
  @Shadow @Final private Minecraft minecraft;

  @Inject(method = "attack", at = @At("HEAD"))
  private void onAttack(Player player, Entity entity, CallbackInfo ci) {
    if (player != minecraft.player) return;

    MaceBoostFeature maceBoost =
        InfiniteClient.INSTANCE.getLocalFeatures().getCombat().getMaceBoostFeature();
    if (maceBoost.isEnabled()) {
      maceBoost.onPreAttack();
    }
  }
}
