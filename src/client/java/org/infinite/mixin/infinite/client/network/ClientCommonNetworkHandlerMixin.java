package org.infinite.mixin.infinite.client.network;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import org.infinite.InfiniteClient;
import org.infinite.global.server.protocol.ProtocolSpoofingSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {
  @Inject(method = "getBrand", at = @At("HEAD"), cancellable = true)
  public void onGetBrand(CallbackInfoReturnable<String> cir) {
    if (InfiniteClient.INSTANCE.isGlobalFeatureEnabled(ProtocolSpoofingSetting.class)) {
      cir.setReturnValue("vanilla");
      cir.cancel();
    }
  }
}
