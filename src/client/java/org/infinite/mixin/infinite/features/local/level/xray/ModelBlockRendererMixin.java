package org.infinite.mixin.infinite.features.local.level.xray;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local; // 重要: Localを使用
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.level.xray.XRayFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ModelBlockRenderer.class, priority = 900)
public class ModelBlockRendererMixin {

  @Unique
  private static XRayFeature xRayFeature() {
    return InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature();
  }

  /** putQuadData へのフック 名前によるキャプチャを廃止し、@Local を使用して型で安全に取得します。 */
  @WrapOperation(
      method = "putQuadData",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[II)V"))
  private static void onPutQuadData(
      VertexConsumer instance,
      PoseStack.Pose pose,
      BakedQuad bakedQuad,
      float[] fs,
      float r,
      float g,
      float b,
      float alpha,
      int[] is,
      int lightmap,
      Operation<Void> original,
      // 引数から型で取得
      @Local(argsOnly = true) BlockState state // 引数から型で取得
      // 引数から型で取得
      ) {
    XRayFeature xRay = xRayFeature();

    if (xRay.isEnabled()) {
      boolean isOre = xRay.getTargetBlocks().getValue().contains(xRay.getBlockId(state));

      float finalAlpha = isOre ? 1.0f : xRay.getTransparency().getValue();
      float brightR = isOre ? 1.0f : r;
      float brightG = isOre ? 1.0f : g;
      float brightB = isOre ? 1.0f : b;

      if (isOre) {
        Arrays.fill(fs, 1.0f);
      }

      original.call(
          instance, pose, bakedQuad, fs, brightR, brightG, brightB, finalAlpha, is, lightmap);
    } else {
      original.call(instance, pose, bakedQuad, fs, r, g, b, alpha, is, lightmap);
    }
  }

  /** shouldRenderFace へのフック */
  @WrapOperation(
      method = "shouldRenderFace",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
  private static boolean onShouldRenderFace(
      BlockState state,
      BlockState neighborState,
      Direction side,
      Operation<Boolean> original,
      @Local(argsOnly = true) BlockAndTintGetter world,
      @Local(argsOnly = true) BlockPos pos,
      @Local(ordinal = 0, argsOnly = true) boolean cull // ローカル変数のboolean(cull)を型と順序で取得
      ) {
    XRayFeature xRay = xRayFeature();
    if (!xRay.isEnabled()) {
      return original.call(state, neighborState, side);
    }

    return xRay.atModelBlockRenderer(
        world, state, cull, side, pos, original.call(state, neighborState, side));
  }
}
