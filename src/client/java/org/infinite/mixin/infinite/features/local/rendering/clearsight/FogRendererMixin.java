package org.infinite.mixin.infinite.features.local.rendering.clearsight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.clearsight.ClearSightFeature;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

  /** FogRenderer 内で霧のデータバッファを更新する直前に介入します。 start と end の距離を極端に大きくすることで、実質的に霧を無効化します。 */
  @WrapOperation(
      method =
          "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
  private void wrapApplyFog(
      FogRenderer instance,
      ByteBuffer buffer,
      int bufPos,
      Vector4f fogColor,
      float environmentalStart,
      float environmentalEnd,
      float renderDistanceStart,
      float renderDistanceEnd,
      float skyEnd,
      float cloudEnd,
      Operation<Void> original) {

    ClearSightFeature feature =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getClearSightFeature();

    if (feature.isEnabled() && feature.getAntiFog().getValue()) {
      float maxFloat = Float.MAX_VALUE;
      renderDistanceStart = maxFloat;
      renderDistanceEnd = maxFloat;
      environmentalStart = maxFloat;
      environmentalEnd = maxFloat;
    }

    original.call(
        instance,
        buffer,
        bufPos,
        fogColor,
        environmentalStart,
        environmentalEnd,
        renderDistanceStart,
        renderDistanceEnd,
        skyEnd,
        cloudEnd);
  }
}
