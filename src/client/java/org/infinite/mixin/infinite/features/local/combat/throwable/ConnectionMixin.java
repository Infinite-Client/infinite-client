package org.infinite.mixin.infinite.features.local.combat.throwable;

import io.netty.channel.ChannelFutureListener;
import kotlin.Unit;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.combat.throwable.ThrowableFeature;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Connection.class, priority = 900)
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
    ThrowableFeature throwableFeature =
        InfiniteClient.INSTANCE.getLocalFeatures().getCombat().getThrowableFeature();

    // 1. 投擲アイテム（卵、パール、雪玉）に関連するパケットか判定
    if (throwableFeature.isEnabled() && isThrowablePacket(packet)) {
      // 指示に基づき、Feature側でステップ分割（maxStep）を考慮して送信を管理
      throwableFeature.handleWrappedLaunch(
          packet,
          channelFutureListener,
          bl,
          ci,
          (p, l, f) -> {
            this.doSendPacket(p, l, f);
            return Unit.INSTANCE;
          });
    }
  }

  /** 投げ物に関連するパケットかどうかを判定 */
  @Unique
  private boolean isThrowablePacket(Packet<?> packet) {
    // 右クリックで使用するパケット
    if (packet instanceof ServerboundUseItemPacket) {
      return true;
    }
    // 特定の状況下（ボウの引き絞り解除など）で投げ物として処理されるアクション
    if (packet instanceof ServerboundPlayerActionPacket actionPacket) {
      return actionPacket.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM;
    }
    return false;
  }
}
