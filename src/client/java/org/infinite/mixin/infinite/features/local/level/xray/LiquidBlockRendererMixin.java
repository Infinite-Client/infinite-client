package org.infinite.mixin.infinite.features.local.level.xray;

import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.level.xray.XRayFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
  /**
   * 流体の面を描画するかどうかの判定をフックします。
   *
   * @param blockState 現在の流体ブロックの状態
   * @param direction チェックしている面
   * @param neighborFluid 隣接するブロックの流体状態
   */
  @Inject(method = "shouldRenderFace", at = @At("RETURN"), cancellable = true)
  private static void onShouldRenderFace(
      FluidState fluidState,
      BlockState blockState,
      Direction direction,
      FluidState neighborFluid,
      CallbackInfoReturnable<Boolean> cir) {
    XRayFeature xRayFeature =
        InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature();

    if (xRayFeature.isEnabled()) {
      cir.setReturnValue(
          xRayFeature.atLiquid(
              fluidState, blockState, direction, neighborFluid, cir.getReturnValue()));
    }
  }
}
