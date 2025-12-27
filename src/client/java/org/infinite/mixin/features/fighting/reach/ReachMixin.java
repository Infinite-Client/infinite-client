package org.infinite.mixin.features.fighting.reach;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.fighting.reach.Reach;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LocalPlayer.class)
public abstract class ReachMixin extends AbstractClientPlayer {

  public ReachMixin(ClientLevel world, GameProfile profile) {
    super(world, profile);
  }

  public double blockInteractionRange() {
    // Feature: Reach
    if (InfiniteClient.INSTANCE.isFeatureEnabled(Reach.class))
      return InfiniteClient.INSTANCE.getSettingFloat(Reach.class, "ReachDistance", 4.5F);

    // super.getBlockInteractionRange()
    return 4.5;
  }

  public double entityInteractionRange() {
    // Feature: Reach
    if (InfiniteClient.INSTANCE.isFeatureEnabled(Reach.class))
      return InfiniteClient.INSTANCE.getSettingFloat(Reach.class, "ReachDistance", 3.0F);

    // super.getEntityInteractionRange()
    return 3.0;
  }
}
