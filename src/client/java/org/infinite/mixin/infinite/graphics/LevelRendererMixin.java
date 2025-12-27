package org.infinite.mixin.infinite.graphics;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.infinite.InfiniteClient;
import org.infinite.feature.ConfigurableFeature;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
  @Inject(
      at = @At("HEAD"),
      method =
          "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V")
  private void onRenderHead(
      GraphicsResourceAllocator allocator,
      DeltaTracker tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Matrix4f matrix4f2,
      GpuBufferSlice gpuBufferSlice,
      Vector4f vector4f,
      boolean bl,
      CallbackInfo ci) {
    InfiniteClient.INSTANCE.handle3dGraphics(
        allocator,
        tickCounter,
        renderBlockOutline,
        camera,
        positionMatrix,
        projectionMatrix,
        matrix4f2,
        gpuBufferSlice,
        vector4f,
        bl,
        ConfigurableFeature.Timing.Start);
  }

  @Inject(
      at = @At("RETURN"),
      method =
          "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V")
  private void onRenderReturn(
      GraphicsResourceAllocator allocator,
      DeltaTracker tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Matrix4f matrix4f2,
      GpuBufferSlice gpuBufferSlice,
      Vector4f vector4f,
      boolean bl,
      CallbackInfo ci) {
    InfiniteClient.INSTANCE.handle3dGraphics(
        allocator,
        tickCounter,
        renderBlockOutline,
        camera,
        positionMatrix,
        projectionMatrix,
        matrix4f2,
        gpuBufferSlice,
        vector4f,
        bl,
        ConfigurableFeature.Timing.End);
  }
}
