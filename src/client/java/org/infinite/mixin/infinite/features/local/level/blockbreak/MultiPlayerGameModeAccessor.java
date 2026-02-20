package org.infinite.mixin.infinite.features.local.level.blockbreak;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
  @Accessor("destroyDelay")
  int getDestroyDelay();

  @Accessor("destroyDelay")
  void setDestroyDelay(int delay);
}
