package org.infinite.mixin.infinite.features.local.level.xray;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

  /** putQuadWithTint 内の BlockQuadOutput.put へのフック */
  @WrapOperation(
      method = "putQuadWithTint",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
  private void onPutQuadWithTint(
      BlockQuadOutput instance,
      float x,
      float y,
      float z,
      BakedQuad bakedQuad,
      QuadInstance quadInstance,
      Operation<Void> original,
      @Local(argsOnly = true) BlockState state) {
    XRayFeature xRay = xRayFeature();

    if (xRay.isEnabled()) {
      boolean isOre = xRay.getTargetBlocks().getValue().contains(xRay.getBlockId(state));

      if (isOre) {
        // 鉱石はそのまま（不透明）
        original.call(instance, x, y, z, bakedQuad, quadInstance);
      } else {
        // 透過設定を取得 (0.0f ~ 1.0f)
        float alpha = xRay.getTransparency().getValue();

        // QuadInstanceの色にアルファ値を乗算する
        // ARGB形式で指定する場合 (Alphaは最上位バイト)
        // 例: 0xFFFFFF に alpha を適用した整数を作成
        int alphaInt = (int) (alpha * 255.0F) << 24;
        int colorMask = 0x00FFFFFF | alphaInt;

        // QuadInstance に色を適用（メソッド名は環境により multiplyColor や setColor の場合があります）
        quadInstance.multiplyColor(colorMask);

        original.call(instance, x, y, z, bakedQuad, quadInstance);
      }
    } else {
      original.call(instance, x, y, z, bakedQuad, quadInstance);
    }
  }

  /** shouldRenderFace へのフック（既存のままで概ね良好ですが、neighborPosとしてキャプチャを修正） */
  @WrapOperation(
      method = "shouldRenderFace",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
  private boolean onShouldRenderFace(
      BlockState state,
      BlockState neighborState,
      Direction side,
      Operation<Boolean> original,
      @Local(argsOnly = true) BlockPos neighborPos // 引数名は neighborPos
      ) {
    XRayFeature xRay = xRayFeature();
    if (!xRay.isEnabled()) {
      return original.call(state, neighborState, side);
    }
    return xRay.atModelBlockRenderer(
        state, side, neighborPos, original.call(state, neighborState, side));
  }
}
