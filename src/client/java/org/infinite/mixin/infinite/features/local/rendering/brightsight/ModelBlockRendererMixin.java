package org.infinite.mixin.infinite.features.local.rendering.brightsight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
      float[] fs, // 各頂点の輝度（影）係数
      float r, // 元々の赤成分（バイオームの色を含む）
      float g, // 元々の緑成分
      float b, // 元々の青成分
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

      // 1. 各頂点の明るさ係数(AO)を最大にする。
      // これにより、ブロックの凹凸による影が消えます。
      Arrays.fill(fs, 1.0f);

      // 2. ライトマップを最大輝度にする
      int fullLight = 0x00EE00EE;
      int noOverlay = OverlayTexture.NO_OVERLAY;
      Arrays.fill(is, noOverlay);
      // 3. 元の r, g, b をそのまま渡す
      // これにより、葉っぱや草の色相が維持されます。
      original.call(instance, pose, bakedQuad, fs, r, g, b, alpha, is, fullLight);
    } else {
      original.call(instance, pose, bakedQuad, fs, r, g, b, alpha, is, lightmap);
    }
  }
}
