package org.infinite.mixin.features.rendering.detailinfo;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.level.block.Blocks;
import org.infinite.InfiniteClient;
import org.infinite.features.rendering.detailinfo.DetailInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

  @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
  private void DetailInfo$cancelOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
    DetailInfo detailInfo = InfiniteClient.INSTANCE.getFeature(DetailInfo.class);
    if (detailInfo != null && detailInfo.getShouldCancelScanScreen()) {
      if (packet.getType() == detailInfo.getExpectedScreenType()) {
        detailInfo.setShouldCancelScanScreen(false);
        ci.cancel();
      } else {
        detailInfo.setShouldCancelScanScreen(false);
        detailInfo.setScanTargetBlockEntity(null);
        detailInfo.setExpectedScreenType(null);
        // Do not cancel, allow normal screen opening
      }
    }
  }

  @Inject(method = "handleContainerContent", at = @At("HEAD"))
  private void DetailInfo$processInventory(
      ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
    if (InfiniteClient.INSTANCE.isSettingEnabled(DetailInfo.class, "InnerChest")) {
      var items = packet.items();
      int syncId = packet.containerId();
      DetailInfo detailInfo = InfiniteClient.INSTANCE.getFeature(DetailInfo.class);
      if (detailInfo != null) {
        detailInfo.handleChestContents(syncId, items);
      }
    }
  }

  @Inject(method = "handleContainerSetData", at = @At("HEAD"))
  private void DetailInfo$processFurnaceProgress(
      ClientboundContainerSetDataPacket packet, CallbackInfo ci) {
    if (InfiniteClient.INSTANCE.isSettingEnabled(DetailInfo.class, "InnerChest")) {
      DetailInfo detailInfo = InfiniteClient.INSTANCE.getFeature(DetailInfo.class);
      var targetDetail = Objects.requireNonNull(detailInfo).getTargetDetail();
      if (targetDetail != null
          && targetDetail.getPos() != null
          && InfiniteClient.INSTANCE.isSettingEnabled(DetailInfo.class, "InnerChest")) {
        var pos = targetDetail.getPos();
        var syncId = packet.getContainerId();
        var propertyId = packet.getId();
        var value = packet.getValue();
        var client = Minecraft.getInstance();
        var world = client.level;
        if (world != null) {
          var blockState = world.getBlockState(targetDetail.getPos());
          if (blockState != null)
            if (blockState.getBlock() == Blocks.BREWING_STAND) {
              detailInfo.handleBrewingProgress(
                  syncId, Objects.requireNonNull(pos), propertyId, value);
            } else if (blockState.getBlock() == Blocks.FURNACE
                || blockState.getBlock() == Blocks.SMOKER
                || blockState.getBlock() == Blocks.BLAST_FURNACE) {
              detailInfo.handleFurnaceProgress(
                  syncId, Objects.requireNonNull(pos), propertyId, value);
            }
        }
      }
    }
  }
}
