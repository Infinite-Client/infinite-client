package org.infinite.mixin.infinite.features.local.combat.throwable;

import io.netty.channel.ChannelFutureListener;
import kotlin.Unit;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.combat.archery.ArcheryFeature;
import org.infinite.infinite.features.local.combat.throwable.ThrowableFeature;
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

  @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
  private void wrapSendPacket(
      Packet<?> packet,
      @Nullable ChannelFutureListener channelFutureListener,
      boolean bl,
      CallbackInfo ci) {

    // 各Featureを取得
    ArcheryFeature archery =
        InfiniteClient.INSTANCE.getLocalFeatures().getCombat().getArcheryFeature();
    ThrowableFeature throwable =
        InfiniteClient.INSTANCE.getLocalFeatures().getCombat().getThrowableFeature();

    // パケットの種類を判定
    boolean isUseItem = packet instanceof ServerboundUseItemPacket;
    boolean isRelease =
        (packet instanceof ServerboundPlayerActionPacket action
            && action.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM);

    if (!isUseItem && !isRelease) return;

    // Archery または Throwable が有効な場合にフック
    if (archery.isEnabled()) {
      archery.handleWrappedLaunch(
          packet,
          channelFutureListener,
          bl,
          (p, l, f) -> {
            this.doSendPacket(p, l, f);
            return Unit.INSTANCE;
          });
      ci.cancel();
    } else if (throwable.isEnabled()) {
      throwable.handleWrappedLaunch(
          packet,
          channelFutureListener,
          bl,
          (p, l, f) -> {
            this.doSendPacket(p, l, f);
            return Unit.INSTANCE;
          });
      ci.cancel();
    }
  }
}
