package org.infinite.mixin.features.fighting.noattack;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.infinite.InfiniteClient;
import org.infinite.features.utils.noattack.NoAttack;
import org.infinite.features.utils.playermanager.PlayerManager;
import org.infinite.settings.FeatureSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class NoAttackMixin {

  @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
  private void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
    NoAttack noAttackFeature = InfiniteClient.INSTANCE.getFeature(NoAttack.class);
    PlayerManager playerManagerFeature = InfiniteClient.INSTANCE.getFeature(PlayerManager.class);

    if (noAttackFeature != null && noAttackFeature.isEnabled()) {
      // Check for protected entities (villagers, pets, etc.)
      FeatureSetting.EntityListSetting protectedEntitiesSetting =
          (FeatureSetting.EntityListSetting) noAttackFeature.getSetting("ProtectedEntities");
      if (protectedEntitiesSetting != null) {
        String targetEntityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        if (protectedEntitiesSetting.getValue().contains(targetEntityId)) {
          ci.cancel(); // Cancel the attack
          return;
        }
      }
    }

    if (playerManagerFeature != null && playerManagerFeature.isEnabled()) {
      // Check for friendly players
      if (target instanceof Player) {
        FeatureSetting.PlayerListSetting friendsSetting =
            (FeatureSetting.PlayerListSetting) playerManagerFeature.getSetting("Friends");
        if (friendsSetting != null) {
          String targetPlayerName = target.getName().getString();
          if (friendsSetting.getValue().contains(targetPlayerName)) {
            ci.cancel(); // Cancel the attack
          }
        }
      }
    }
  }
}
