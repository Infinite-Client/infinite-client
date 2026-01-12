package org.infinite.mixin.infinite.features.local.rendering.xray;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
  @Unique
  private static XRayFeature xRayFeature() {
    return InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature();
  }

  @Unique
  private static boolean isEnabled() {
    XRayFeature feature = xRayFeature();
    return feature.isEnabled();
  }

  @WrapOperation(
      method = "putQuadData",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[II)V"))
  private static void onPuQuadData(
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
      BlockAndTintGetter world,
      BlockState state,
      BlockPos pos) {
    XRayFeature xRay = xRayFeature();

    if (xRay.isEnabled()) {
      boolean isOre = xRay.getTargetBlocks().getValue().contains(xRay.getBlockId(state));

      // 1. 透明度: 鉱石は不透明、石は透明
      float finalAlpha = isOre ? 1.0f : xRay.getTransparency().getValue();

      // 2. 【重要】発光処理:
      // 通常、r, g, b には AO (影) の係数が掛かっており、1.0F より小さい値になっています。
      // これを 1.0F に書き換えることで、影を消し去り「発光」させます。
      float brightR = isOre ? 1.0f : r;
      float brightG = isOre ? 1.0f : g;
      float brightB = isOre ? 1.0f : b;

      // 3. 配列内の明るさ (fs) も、もしあれば 1.0F で埋める
      // 多くのバージョンで fs[0]〜fs[3] は各頂点の明るさ(brightness)です。
      if (isOre) {
        Arrays.fill(fs, 1.0f);
      }

      original.call(
          instance, pose, bakedQuad, fs, brightR, brightG, brightB, finalAlpha, is, lightmap);
    } else {
      original.call(instance, pose, bakedQuad, fs, r, g, b, alpha, is, lightmap);
    }
  }

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
      BlockAndTintGetter world,
      BlockState stateButDuplicate,
      boolean cull,
      Direction sideButDuplicate,
      BlockPos pos) {
    if (!isEnabled()) {
      return original.call(state, neighborState, side);
    }

    // XRay有効時は、XRayFeature側のロジック（特定ブロックの表示/非表示）を優先
    return xRayFeature()
        .atModelBlockRenderer(
            world, state, cull, side, pos, original.call(state, neighborState, side));
  }
}
