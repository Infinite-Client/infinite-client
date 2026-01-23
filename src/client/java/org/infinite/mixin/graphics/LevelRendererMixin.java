package org.infinite.mixin.graphics;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.infinite.libs.core.tick.RenderTicks;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
  @Inject(
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/debug/GameTestBlockHighlightRenderer;emitGizmos()V",
              shift = At.Shift.AFTER),
      method =
          "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V")
  private void onRender(
      GraphicsResourceAllocator graphicsResourceAllocator,
      DeltaTracker deltaTracker,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Matrix4f frustumMatrix,
      GpuBufferSlice gpuBufferSlice,
      Vector4f vector4f,
      boolean bl2,
      CallbackInfo ci) {
    RenderTicks.INSTANCE.onLevelRendering(
        graphicsResourceAllocator,
        deltaTracker,
        renderBlockOutline,
        camera,
        positionMatrix,
        projectionMatrix,
        frustumMatrix,
        gpuBufferSlice,
        vector4f,
        bl2);
  }
}
