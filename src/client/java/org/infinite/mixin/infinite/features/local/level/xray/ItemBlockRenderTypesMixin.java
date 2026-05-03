package org.infinite.mixin.infinite.features.local.level.xray;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.infinite.InfiniteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SectionCompiler.class)
public class ItemBlockRenderTypesMixin {

  @ModifyVariable(method = "getOrBeginLayer", at = @At("HEAD"), argsOnly = true, name = "layer")
  private static ChunkSectionLayer onGetChunkRenderType(ChunkSectionLayer layer) {
    var xRay = InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature();

    if (xRay.isEnabled()) {
      return ChunkSectionLayer.TRANSLUCENT;
    }
    return layer;
  }
}
