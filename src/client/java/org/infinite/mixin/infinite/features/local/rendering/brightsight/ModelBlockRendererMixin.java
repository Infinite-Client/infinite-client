package org.infinite.mixin.infinite.features.local.rendering.brightsight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.brightsight.BrightSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

  @Unique
  private static BrightSightFeature brightSight() {
    return InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getBrightSightFeature();
  }

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
      BlockAndTintGetter world,
      BlockState state,
      BlockPos pos) {

    BrightSightFeature feature = brightSight();

    if (feature.isEnabled()
        && (feature.getMethod().getValue() == BrightSightFeature.Method.GamMax
            || feature.getMethod().getValue() == BrightSightFeature.Method.UltraBright)) {

      // 1. 影(Ambient Occlusion)を消し去る
      // 通常、r,g,bは0.2〜1.0の間で影の強さに応じて変動しますが、1.0fに固定することで全画面が明るくなります。
      float brightR = 1.0f;
      float brightG = 1.0f;
      float brightB = 1.0f;

      // 2. 各頂点の明るさ係数も最大(1.0f)で埋める
      Arrays.fill(fs, 1.0f);

      // 3. ライトマップ(内部的な明るさ値)を最大にする
      // 0xF000F0 は Minecraft における最大輝度（空の明るさ15 + ブロックの明るさ15）です。
      int fullLight = 15728880; // 0xF000F0

      original.call(instance, pose, bakedQuad, fs, brightR, brightG, brightB, alpha, is, fullLight);
    } else {
      original.call(instance, pose, bakedQuad, fs, r, g, b, alpha, is, lightmap);
    }
  }
}
