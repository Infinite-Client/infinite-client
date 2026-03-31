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
      @Local(argsOnly = true, name = "state") BlockState state) {

    XRayFeature xRay = xRayFeature();

    if (xRay.isEnabled()) {
      boolean isOre = xRay.getTargetBlocks().getValue().contains(xRay.getBlockId(state));

      if (isOre) {
        // 先に元の処理を呼び出す（ここで本来の暗いライトマップがセットされる）
        original.call(instance, x, y, z, bakedQuad, quadInstance);

        // --- 呼び出し直後に最大輝度で上書き ---
        // 15728880 は 0xF000F0 (Full Bright) です
        quadInstance.setLightCoords(15728880);

        // もし色の乗算(シェーディング)で暗くなっている場合は、色も白(-1)にリセット
        quadInstance.setColor(-1);
      } else {
        // 透過設定
        float alpha = xRay.getTransparency().getValue();
        int alphaInt = (int) (alpha * 255.0F) << 24;
        int colorMask = 0x00FFFFFF | alphaInt;

        quadInstance.multiplyColor(colorMask);
        original.call(instance, x, y, z, bakedQuad, quadInstance);
      }
    } else {
      original.call(instance, x, y, z, bakedQuad, quadInstance);
    }
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
      Direction side,
      Operation<Boolean> original,
      @Local(argsOnly = true, name = "neighborPos") BlockPos neighborPos // 引数名は neighborPos
      ) {
    XRayFeature xRay = xRayFeature();
    if (!xRay.isEnabled()) {
      return original.call(state, neighborState, side);
    }
    return xRay.atModelBlockRenderer(
        state, side, neighborPos, original.call(state, neighborState, side));
  }
}
