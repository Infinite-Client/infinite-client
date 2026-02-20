package org.infinite.mixin.core.networking;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.infinite.libs.level.LevelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

  @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
  private void onHandleBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
    LevelManager.INSTANCE.handleBlockUpdate(packet);
  }

  @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
  private void onHandleChunkBlocksUpdate(
      ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
    LevelManager.INSTANCE.handleDeltaUpdate(packet);
  }

  @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
  private void onHandleLevelChunkWithLight(
      ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
    LevelManager.INSTANCE.handleChunkLoad(packet.getX(), packet.getZ(), packet.getChunkData());
  }
}
