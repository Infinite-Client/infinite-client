package org.infinite.mixin.core;

import net.minecraft.client.Minecraft;
import org.infinite.libs.core.tick.SystemTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
class MinecraftMixin {
  @Inject(at = @At("HEAD"), method = "runTick")
  private void onStartTick(boolean bl, CallbackInfo ci) {
    SystemTicks.INSTANCE.onStartTick();
  }

  @Inject(at = @At("TAIL"), method = "runTick")
  private void onEndTick(boolean bl, CallbackInfo ci) {
    SystemTicks.INSTANCE.onEndTick();
  }
}
