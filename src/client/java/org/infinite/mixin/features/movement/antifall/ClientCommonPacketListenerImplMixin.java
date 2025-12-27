package org.infinite.mixin.features.movement.antifall;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.infinite.InfiniteClient;
import org.infinite.features.movement.fall.AntiFall;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin implements ClientCommonPacketListener {
  @WrapOperation(
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"),
      method = "send(Lnet/minecraft/network/protocol/Packet;)V")
  private void wrapSendPacket(Connection connection, Packet<?> packet, Operation<Void> original) {
    var client = Minecraft.getInstance();
    if ((client.player != null)
        && InfiniteClient.INSTANCE.isFeatureEnabled(AntiFall.class)
        && (client.gameMode != null)
        && (packet instanceof ServerboundMovePlayerPacket playerMoveC2SPacket)
        && (!client.player.isFallFlying())
        && (!client.player.isShiftKeyDown())) {
      var targetPacket = onGroundPacket(playerMoveC2SPacket);
      original.call(connection, targetPacket);
    } else {
      original.call(connection, packet);
    }
  }

  @Unique
  private ServerboundMovePlayerPacket onGroundPacket(ServerboundMovePlayerPacket packet) {
    if (packet instanceof ServerboundMovePlayerPacket.PosRot)
      return new ServerboundMovePlayerPacket.PosRot(
          packet.getX(0),
          packet.getY(0),
          packet.getZ(0),
          packet.getYRot(0),
          packet.getXRot(0),
          true,
          packet.horizontalCollision());

    if (packet instanceof ServerboundMovePlayerPacket.Pos)
      return new ServerboundMovePlayerPacket.Pos(
          packet.getX(0), packet.getY(0), packet.getZ(0), true, packet.horizontalCollision());

    if (packet instanceof ServerboundMovePlayerPacket.Rot)
      return new ServerboundMovePlayerPacket.Rot(
          packet.getYRot(0), packet.getXRot(0), true, packet.horizontalCollision());

    return new ServerboundMovePlayerPacket.StatusOnly(true, packet.horizontalCollision());
  }
}
