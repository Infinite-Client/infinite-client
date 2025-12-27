package org.infinite.mixin.infinite.client.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.infinite.InfiniteClient;
import org.infinite.global.server.protocol.ProtocolSpoofingSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {

  // TODO(Ravel): wildcard and regex target are not supported
  // TODO(Ravel): wildcard and regex target are not supported
  // TODO(Ravel): wildcard and regex target are not supported
  // TODO(Ravel): wildcard and regex target are not supported
  // TODO(Ravel): wildcard and regex target are not supported
  // TODO(Ravel): wildcard and regex target are not supported
  // TODO(Ravel): wildcard and regex target are not supported
  @Inject(method = "send*", at = @At("HEAD"), cancellable = true)
  private void infiniteClient$onSendPacket(Packet<?> packet, CallbackInfo ci) {
    ProtocolSpoofingSetting protocolSpoofingSetting =
        InfiniteClient.INSTANCE.getGlobalFeature(ProtocolSpoofingSetting.class);
    if (protocolSpoofingSetting != null && protocolSpoofingSetting.isEnabled()) {
      if (packet instanceof ServerboundCustomPayloadPacket) {
        ci.cancel();
      }
    }
  }
}
