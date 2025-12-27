package org.infinite.mixin.features.rendering.xray;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.infinite.InfiniteClient;
import org.infinite.features.rendering.xray.XRay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LiquidBlockRenderer.class)
public class XRayLiquidBlockRendererMixin {

  /** Hides and shows fluids when using X-Ray without Sodium installed. */
  @WrapOperation(
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"),
      method =
          "tesselate(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V")
  private boolean modifyShouldSkipRendering(
      Direction side, // shouldSkipRenderingの引数1: チェックしている面
      float height, // shouldSkipRenderingの引数2
      BlockState neighborState, // shouldSkipRenderingの引数3: 隣接ブロックの状態
      Operation<Boolean> original,
      BlockAndTintGetter world,
      BlockPos pos, // レンダリング対象の流体ブロックの座標
      VertexConsumer vertexConsumer,
      BlockState blockState, // レンダリング対象の流体ブロックの状態
      FluidState fluidState) {
    XRay xray = InfiniteClient.INSTANCE.getFeature(XRay.class);

    // XRayが無効、または取得できない場合は、オリジナルのメソッドを呼び出して終了
    if (xray == null || !InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
      return original.call(side, height, neighborState);
    }

    // shouldDrawSideを新しいシグネチャで呼び出す
    // shouldDrawSide(現在のブロックの状態, 現在のブロックの座標, チェックしている面, 隣接ブロックの状態)
    // FluidRenderer.shouldSkipRenderingは、レンダリング対象の流体ブロックに対して、
    // 特定の方向(side)の面を描画するかどうかを判断するために隣接ブロック(neighborState)をチェックする。
    Boolean shouldDraw = xray.shouldDrawSide(blockState, pos, side, neighborState);

    // XRay機能が描画ロジックをオーバーライドする場合
    if (shouldDraw != null) {
      // shouldDrawSideは「描画すべきかどうか」を返す。
      // shouldSkipRenderingは「描画をスキップすべきかどうか」を返すので、論理を反転させる。
      return !shouldDraw;
    }

    // XRay機能が判断しなかった場合は、オリジナルのメソッドを呼び出す
    return original.call(side, height, neighborState);
  }
}
