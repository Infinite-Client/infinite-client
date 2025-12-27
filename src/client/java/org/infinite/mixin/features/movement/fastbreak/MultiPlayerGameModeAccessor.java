package org.infinite.mixin.features.movement.fastbreak;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
  @Accessor("destroyDelay")
  void setDestroyDelay(int cooldown);
}
