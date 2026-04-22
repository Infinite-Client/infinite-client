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
      @Local(argsOnly = true, name = "state") BlockState state) {

    XRayFeature xRay = xRayFeature();

    if (xRay.isEnabled()) {
      boolean isOre = xRay.getTargetBlocks().getValue().contains(xRay.getBlockId(state));

      if (isOre) {
        // --- original.call の「前」に設定する ---
        // ライトマップを最大に固定（暗い場所でも光る）
        quadInstance.setLightCoords(15728880);

        // シェーディング（影）を無効化したい場合は白に設定
        // ただし quadInstance.setColor(i, color) をループで回す方が確実な場合がある
        for (int i = 0; i < 4; i++) {
          quadInstance.setColor(i, -1);
        }
      } else {
        // 透過設定（石など）
        float alpha = xRay.getTransparency().getValue();
        int a = (int) (alpha * 255.0F);

        for (int i = 0; i < 4; i++) {
          int oldColor = quadInstance.getColor(i);
          // 元のRGBを維持しつつ、A（アルファ）だけを書き換える
          int newColor = (a << 24) | (oldColor & 0x00FFFFFF);
          quadInstance.setColor(i, newColor);
        }
      }
    }

    // 最後に一回だけ呼び出す
    original.call(instance, x, y, z, bakedQuad, quadInstance);
  }

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
      Direction direction,
      Operation<Boolean> original,
      @Local(argsOnly = true, name = "neighborPos") BlockPos neighborPos // 引数名は neighborPos
      ) {
    XRayFeature xRay = xRayFeature();
    if (!xRay.isEnabled()) {
      return original.call(state, neighborState, direction);
    }
    return xRay.atModelBlockRenderer(
        state, direction, neighborPos, original.call(state, neighborState, direction));
  }
}
