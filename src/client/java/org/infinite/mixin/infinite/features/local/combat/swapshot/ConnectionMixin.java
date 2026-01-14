package org.infinite.mixin.infinite.features.local.combat.swapshot;

import net.minecraft.network.Connection;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.combat.swapshot.SwapShotFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public class ConnectionMixin {
  @Unique
  private SwapShotFeature swapShotFeature() {
    return InfiniteClient.INSTANCE.getLocalFeatures().getCombat().getSwapShotFeature();
  }
}
