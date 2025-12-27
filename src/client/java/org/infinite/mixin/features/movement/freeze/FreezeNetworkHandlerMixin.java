package org.infinite.mixin.features.movement.freeze;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.infinite.InfiniteClient;
import org.infinite.features.movement.freeze.Freeze;
import org.infinite.features.rendering.camera.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class FreezeNetworkHandlerMixin {

  /**
   * クライアントがパケットを送信する直前にフックし、Freezeが有効な場合は処理をブロックする。
   *
   * @param packet 送信されようとしているパケット
   * @param ci CallbackInfo
   */
  @Inject(
      method = "send(Lnet/minecraft/network/protocol/Packet;)V",
      at = @At("HEAD"),
      cancellable = true)
  private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
    if (InfiniteClient.INSTANCE.isFeatureEnabled(FreeCamera.class)) {
      ci.cancel(); // Cancel the original method, preventing Freeze from processing packets
      return;
    }
    Freeze freezeFeature = InfiniteClient.INSTANCE.getFeature(Freeze.class);
    if (!InfiniteClient.INSTANCE.isFeatureEnabled(Freeze.class)
        || !(packet instanceof ServerboundMovePlayerPacket)
        || freezeFeature == null) {
      return;
    }
    freezeFeature.processMovePacket((ServerboundMovePlayerPacket) packet);
    ci.cancel();
  }
}
