package org.infinite.mixin.infinite.features.local.level.xray;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.level.xray.XRayFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelLighter.class)
public class BlockModelLighterMixin {
  @Unique
  private XRayFeature feature() {
    return InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature();
  }

  @Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
  private void onGetLightCoords(
      BlockState state,
      BlockAndTintGetter level,
      BlockPos relativePos,
      CallbackInfoReturnable<Integer> cir) {
    if (feature().isEnabled() && !feature().shouldIsolate(state)) {
      // 15728880 (0x00F000F0) を返して計算を中断させる
      cir.setReturnValue(15728880);
    }
  }

  @Inject(method = "prepareQuadAmbientOcclusion", at = @At("TAIL"))
  private void onPrepareQuadAmbientOcclusion(
      BlockAndTintGetter level,
      BlockState state,
      BlockPos centerPosition,
      BakedQuad quad,
      QuadInstance outputInstance,
      CallbackInfo ci) {
    if (feature().isEnabled() && !feature().shouldIsolate(state)) {
      outputInstance.scaleColor(1.0f);
      outputInstance.setLightCoords(15728880);
    }
  }
}
