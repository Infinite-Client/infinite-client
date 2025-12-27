package org.infinite.mixin.infinite.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.infinite.InfiniteClient;
import org.infinite.libs.world.WorldManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl
    implements TickablePacketListener, ClientGamePacketListener {
  @Unique
  private WorldManager worldManager() {
    return InfiniteClient.INSTANCE.getWorldManager();
  }

  private ClientPacketListenerMixin(
      Minecraft client, Connection connection, CommonListenerCookie connectionState) {
    super(client, connection, connectionState);
  }

  @Inject(
      at = @At("TAIL"),
      method =
          "updateLevelChunk(IILnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;)V")
  private void onLoadChunk(
      int x, int z, ClientboundLevelChunkPacketData chunkData, CallbackInfo ci) {
    worldManager().handleChunkLoad(x, z, chunkData);
  }

  @Inject(
      at = @At("TAIL"),
      method =
          "handleBlockUpdate(Lnet/minecraft/network/protocol/game/ClientboundBlockUpdatePacket;)V")
  private void onOnBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
    worldManager().handleBlockUpdate(packet);
  }

  @Inject(
      at = @At("TAIL"),
      method =
          "handleChunkBlocksUpdate(Lnet/minecraft/network/protocol/game/ClientboundSectionBlocksUpdatePacket;)V")
  private void onOnChunkDeltaUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
    worldManager().handleDeltaUpdate(packet);
  }
}
