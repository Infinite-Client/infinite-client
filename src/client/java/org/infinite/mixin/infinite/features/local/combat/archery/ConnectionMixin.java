package org.infinite.mixin.infinite.features.local.combat.archery;

import io.netty.channel.ChannelFutureListener;
import kotlin.Unit;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.combat.archery.ArcheryFeature;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

  @Shadow
  protected abstract void doSendPacket(
      Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, boolean bl);

  @Inject(
      method = "sendPacket", // もし通らない場合は "method = { \"sendPacket\", \"m_...\" }" のように難読化名も検討
      at = @At("HEAD"),
      cancellable = true)
  private void wrapSendPacket(
      Packet<?> packet,
      @Nullable ChannelFutureListener channelFutureListener,
      boolean bl,
      CallbackInfo ci) {
    ArcheryFeature archeryFeature =
        InfiniteClient.INSTANCE.getLocalFeatures().getCombat().getArcheryFeature();

    if (archeryFeature.isEnabled()) {
      boolean isLaunch = false;
      if (packet instanceof ServerboundPlayerActionPacket action
          && action.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
        isLaunch = true;
      } else if (packet instanceof ServerboundUseItemPacket) {
        isLaunch = true;
      }

      if (isLaunch) {
        archeryFeature.handleWrappedLaunch(
            packet,
            channelFutureListener,
            bl,
            ci,
            (p, l, f) -> {
              // ここで packet (元のパケット) ではなく、p (渡されたパケット) を使う必要があります
              this.doSendPacket(p, l, f);
              return Unit.INSTANCE;
            });
      }
    }
  }
}
