package org.infinite.mixin.features.fighting.hypermace;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.infinite.InfiniteClient;
import org.infinite.features.fighting.mace.HyperMace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class HyperMaceMixin {

  @Inject(method = "attack", at = @At("HEAD"))
  private void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
    HyperMace hyperMaceFeature = InfiniteClient.INSTANCE.getFeature(HyperMace.class);

    if (hyperMaceFeature == null || !hyperMaceFeature.isEnabled()) {
      return;
    }
    hyperMaceFeature.execute(player, target);
  }
}
