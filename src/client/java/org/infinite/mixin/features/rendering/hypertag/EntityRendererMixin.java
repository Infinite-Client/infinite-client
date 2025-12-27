package org.infinite.mixin.features.rendering.hypertag;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.rendering.tag.HyperTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<S extends EntityRenderState> {

  @Inject(method = "submitNameTag", at = @At(value = "HEAD"), cancellable = true)
  private void cancelLabelRendering(
      S renderState,
      PoseStack matrices,
      SubmitNodeCollector queue,
      CameraRenderState cameraRenderState,
      CallbackInfo ci) {
    if (InfiniteClient.INSTANCE.isFeatureEnabled(HyperTag.class)) {
      ci.cancel();
    }
  }
}
