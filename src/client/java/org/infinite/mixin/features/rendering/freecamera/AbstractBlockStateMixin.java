package org.infinite.mixin.features.rendering.freecamera;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.infinite.InfiniteClient;
import org.infinite.features.rendering.camera.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AbstractBlockStateMixin {

  @Inject(
      at = @At("RETURN"),
      method =
          "isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
      cancellable = true)
  private void onIsFullCube(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    if (InfiniteClient.INSTANCE.isFeatureEnabled(FreeCamera.class)) cir.setReturnValue(false);
  }
}
