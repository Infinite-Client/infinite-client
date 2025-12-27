package org.infinite.mixin.features.movement.builder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.infinite.InfiniteClient;
import org.infinite.features.movement.builder.Builder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class BuilderMixin {
  @Shadow private GameType localPlayerMode;

  // エンティティ攻撃パケットをキャンセル
  @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
  private void infinite$cancelAttackEntity(Player player, Entity target, CallbackInfo ci) {
    if (InfiniteClient.INSTANCE.isFeatureEnabled(Builder.class)) {
      ci.cancel(); // Builderが有効な場合、エンティティ攻撃をキャンセル
    }
  }

  // ブロックインタラクト時に、Builder機能が有効であればBlockHitResultを変更
  @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
  private void infinite$redirectInteractBlock(
      LocalPlayer player,
      InteractionHand hand,
      BlockHitResult hitResult,
      CallbackInfoReturnable<InteractionResult> cir) {
    Builder builderFeature = InfiniteClient.INSTANCE.getFeature(Builder.class);
    if (builderFeature != null && builderFeature.isEnabled()) {
      BlockHitResult modifiedHitResult = getBlockHitResult(hitResult, builderFeature);

      // 修正されたBlockHitResultを使用して内部のインタラクトブロックメソッドを呼び出す
      InteractionResult result = interactBlockInternal(player, hand, modifiedHitResult);
      cir.setReturnValue(result);
      cir.cancel(); // Cancel the original method as we've handled it
    }
  }

  @Unique
  private static @NotNull BlockHitResult getBlockHitResult(
      BlockHitResult hitResult, Builder builderFeature) {
    BlockPos originalPos = hitResult.getBlockPos();
    Direction currentOffset = builderFeature.getCurrentPlacementOffset();

    // 新しいBlockPosを計算
    BlockPos newPos = originalPos.relative(currentOffset);

    // 新しいBlockHitResultを作成 (面は元のヒット結果の面を使用)
    return new BlockHitResult(
        new Vec3(newPos.getX() + 0.5, newPos.getY() + 0.5, newPos.getZ() + 0.5),
        hitResult.getDirection(), // 元のヒット面のまま
        newPos,
        hitResult.isInside());
  }

  @Unique
  private Minecraft client() {
    return Minecraft.getInstance();
  }

  @Unique
  private InteractionResult interactBlockInternal(
      LocalPlayer player, InteractionHand hand, BlockHitResult hitResult) {
    BlockPos blockPos = hitResult.getBlockPos();
    ItemStack itemStack = player.getItemInHand(hand);
    if (this.localPlayerMode == GameType.SPECTATOR) {
      return InteractionResult.CONSUME;
    } else {
      boolean bl = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
      boolean bl2 = player.isSecondaryUseActive() && bl;
      ClientLevel world = client().level;
      ClientPacketListener networkHandler = client().getConnection();
      if (!bl2 && world != null && networkHandler != null) {
        BlockState blockState = world.getBlockState(blockPos);
        if (!networkHandler.isFeatureEnabled(blockState.getBlock().requiredFeatures())) {
          return InteractionResult.FAIL;
        }

        InteractionResult actionResult =
            blockState.useItemOn(
                player.getItemInHand(hand), client().level, player, hand, hitResult);
        if (actionResult.consumesAction()) {
          return actionResult;
        }

        if (actionResult instanceof InteractionResult.TryEmptyHandInteraction
            && hand == InteractionHand.MAIN_HAND) {
          InteractionResult actionResult2 =
              blockState.useWithoutItem(client().level, player, hitResult);
          if (actionResult2.consumesAction()) {
            return actionResult2;
          }
        }
      }

      if (!itemStack.isEmpty() && !player.getCooldowns().isOnCooldown(itemStack)) {
        UseOnContext itemUsageContext = new UseOnContext(player, hand, hitResult);
        InteractionResult actionResult3;
        if (player.hasInfiniteMaterials()) {
          int i = itemStack.getCount();
          actionResult3 = itemStack.useOn(itemUsageContext);
          itemStack.setCount(i);
        } else {
          actionResult3 = itemStack.useOn(itemUsageContext);
        }

        return actionResult3;
      } else {
        return InteractionResult.PASS;
      }
    }
  }
}
