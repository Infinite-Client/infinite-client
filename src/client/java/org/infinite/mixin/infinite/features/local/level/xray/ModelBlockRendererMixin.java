package org.infinite.mixin.infinite.features.local.level.xray;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        // --- Ores: Force full brightness ---
        quadInstance.setLightCoords(15728880);
        for (int i = 0; i < 4; i++) {
          quadInstance.setColor(i, -1);
        }
      } else {
        // --- Non-target blocks: Apply transparency ---
        float alpha = xRay.getTransparency().getValue();
        int a = (int) (alpha * 255.0F);

        for (int i = 0; i < 4; i++) {
          int oldColor = quadInstance.getColor(i);
          int newColor = (a << 24) | (oldColor & 0x00FFFFFF);
          quadInstance.setColor(i, newColor);
        }
      }
    }

    // Call original (e.g., BrightSight)
    original.call(instance, x, y, z, bakedQuad, quadInstance);

    // Re-apply if necessary (BrightSight might have overwritten colors/lights)
    if (xRay.isEnabled()) {
      boolean isOre = xRay.getTargetBlocks().getValue().contains(xRay.getBlockId(state));
      if (isOre) {
        quadInstance.setLightCoords(15728880);
      } else {
        float alpha = xRay.getTransparency().getValue();
        int a = (int) (alpha * 255.0F);
        for (int i = 0; i < 4; i++) {
          int oldColor = quadInstance.getColor(i);
          int newColor = (a << 24) | (oldColor & 0x00FFFFFF);
          quadInstance.setColor(i, newColor);
        }
      }
    }
  }

  @Inject(method = "shouldRenderFace", at = @At("RETURN"), cancellable = true)
  private void onShouldRenderFace(
      BlockAndTintGetter level,
      BlockState state,
      Direction direction,
      BlockPos neighborPos,
      CallbackInfoReturnable<Boolean> cir) {
    XRayFeature xRay = xRayFeature();
    if (xRay.isEnabled()) {
      // neighborPos は pos.relative(direction) なので、逆方向に移動して pos を求める
      BlockPos pos = neighborPos.relative(direction.getOpposite());
      cir.setReturnValue(xRay.atModelBlockRenderer(state, direction, pos, cir.getReturnValue()));
    }
  }
}
