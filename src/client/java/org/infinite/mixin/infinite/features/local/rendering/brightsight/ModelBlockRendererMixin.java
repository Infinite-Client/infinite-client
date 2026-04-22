package org.infinite.mixin.infinite.features.local.rendering.brightsight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
      method = "putQuadWithTint",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
  private void onPutQuadData(
      BlockQuadOutput instance,
      float x,
      float y,
      float z,
      net.minecraft.client.resources.model.geometry.BakedQuad bakedQuad,
      QuadInstance quadInstance,
      Operation<Void> original) {
    BrightSightFeature feature = brightSight();

    if (feature.isEnabled()
        && (feature.getMethod().getValue() == BrightSightFeature.Method.GamMax
            || feature.getMethod().getValue() == BrightSightFeature.Method.UltraBright)) {

      // 1. 各頂点の明るさ係数(AO)を最大にする。
      // scaleColor(1.0f) は色を維持しつつ、影（Ambient Occlusion）による減衰をリセットします。
      quadInstance.scaleColor(1.0f);

      // 2. ライトマップを最大輝度にする。
      // バニラのフルブライト定数値。15728880 は 0x00F000F0 と同等です。
      int fullLight = 0x00F000F0;
      quadInstance.setLightCoords(fullLight);

      // 3. オーバーレイ（ダメージ時の赤みなど）を無効化（必要に応じて）
      quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

      // 修正した quadInstance を使って元の処理を呼び出す
      original.call(instance, x, y, z, bakedQuad, quadInstance);
    } else {
      // 通常時
      original.call(instance, x, y, z, bakedQuad, quadInstance);
    }
  }
}
