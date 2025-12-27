package org.infinite.mixin.features.movement.brawler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.infinite.InfiniteClient;
import org.infinite.features.movement.brawler.Brawler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class BrawlerMixin {

  // Suppress for MinecraftClient.getInstance()
  @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
  private void infinite$onInteractBlock(
      net.minecraft.client.player.LocalPlayer player,
      InteractionHand hand,
      BlockHitResult hitResult,
      CallbackInfoReturnable<InteractionResult> cir) {
    Brawler brawerFeature = InfiniteClient.INSTANCE.getFeature(Brawler.class);

    if (brawerFeature != null && brawerFeature.isEnabled()) {
      Level world = Minecraft.getInstance().level;
      player.getItemInHand(hand);
      if (world != null) {
        String blockId =
            BuiltInRegistries.BLOCK
                .getKey(world.getBlockState(hitResult.getBlockPos()).getBlock())
                .toString();

        if (brawerFeature.getInteractCancelBlocks().getValue().contains(blockId)) {
          // Interact with the currently held item instead of the block
          player.connection.send(new ServerboundUseItemPacket(hand, 0, 0.0f, 0.0f));

          // Return SUCCESS to prevent further processing of the block interaction
          cir.setReturnValue(InteractionResult.SUCCESS);
          cir.cancel();
        }
      }
    }
  }
}
