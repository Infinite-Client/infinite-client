package org.infinite.mixin.infinite.features.local.combat.counter;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.combat.counter.CounterFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class CounterFeatureMixin {

  /** ClientboundDamageEventPacket (旧 EntityDamageS2CPacket) を受信した際に実行 */
  @Inject(method = "handleDamageEvent", at = @At("HEAD"))
  private void infinite$onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
    // CounterFeature インスタンスを取得
    // getLocalFeatures().getCombat().getCounter() の構造はプロジェクトの定義に合わせてください
    CounterFeature counterFeature =
        InfiniteClient.INSTANCE.getLocalFeatures().getCombat().getCounterFeature();

    // counterFeature.isEnabled() は内部でチェックしているため、そのまま呼び出しても安全です
    counterFeature.onDamageReceived(packet);
  }
}
