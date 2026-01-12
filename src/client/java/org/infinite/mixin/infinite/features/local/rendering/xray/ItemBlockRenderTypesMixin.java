package org.infinite.mixin.infinite.features.local.rendering.xray;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.infinite.InfiniteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {

  @Inject(method = "getChunkRenderType", at = @At("HEAD"), cancellable = true)
  private static void onGetChunkRenderType(
      BlockState blockState, CallbackInfoReturnable<ChunkSectionLayer> cir) {
    var xRay = InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature();

    if (xRay.isEnabled()) {
      if (xRay.shouldIsolate(blockState)) {
        cir.setReturnValue(ChunkSectionLayer.TRANSLUCENT);
      } else {
        // 鉱石などはそのまま（SOLIDなど）で描画させる
        cir.setReturnValue(ChunkSectionLayer.SOLID);
      }
    }
  }
}
