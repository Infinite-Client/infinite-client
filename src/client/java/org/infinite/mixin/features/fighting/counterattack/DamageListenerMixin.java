package org.infinite.mixin.features.fighting.counterattack;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.fighting.counter.CounterAttack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class DamageListenerMixin {

  @Inject(method = "handleDamageEvent", at = @At("HEAD"))
  private void infinite$onEntityDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
    CounterAttack counterAttack = InfiniteClient.INSTANCE.getFeature(CounterAttack.class);
    if (counterAttack != null && counterAttack.isEnabled()) {
      counterAttack.receive(packet);
    }
  }
}
