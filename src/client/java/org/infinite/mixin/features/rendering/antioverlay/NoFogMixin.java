package org.infinite.mixin.features.rendering.antioverlay;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.infinite.InfiniteClient;
import org.infinite.features.rendering.overlay.AntiOverlay;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public class NoFogMixin {
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
    if (InfiniteClient.INSTANCE.isSettingEnabled(AntiOverlay.class, "NoFogOverlay")) {
      renderDistanceStart = Integer.MAX_VALUE;
      renderDistanceEnd = Integer.MAX_VALUE;
      environmentalStart = Integer.MAX_VALUE;
      environmentalEnd = Integer.MAX_VALUE;
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
