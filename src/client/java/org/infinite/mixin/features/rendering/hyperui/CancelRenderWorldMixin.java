package org.infinite.mixin.features.rendering.hyperui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.infinite.InfiniteClient;
import org.infinite.features.rendering.ui.HyperUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class CancelRenderWorldMixin {
  @Unique
  private boolean shouldCancel() {
    // nullチェックは省略していますが、必要に応じてInfiniteClient.INSTANCEがnullでないことを確認してください
    return InfiniteClient.INSTANCE.isFeatureEnabled(HyperUi.class);
  }

  @WrapOperation(
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/BlockOutlineRenderState;IF)V"),
      method =
          "renderBlockOutline(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;ZLnet/minecraft/client/renderer/state/LevelRenderState;)V")
  private void cancelBlockOutline(
      LevelRenderer instance,
      PoseStack matrices,
      VertexConsumer vertexConsumer,
      double x,
      double y,
      double z,
      BlockOutlineRenderState state,
      int i,
      float lineWidth,
      Operation<Void> original) {
    if (shouldCancel()) {
      InfiniteClient inf = InfiniteClient.INSTANCE;
      int modifyColor = inf.theme(inf.getCurrentTheme()).getColors().getPrimaryColor();
      original.call(instance, matrices, vertexConsumer, x, y, z, state, modifyColor, lineWidth);
    } else {
      original.call(instance, matrices, vertexConsumer, x, y, z, state, i, lineWidth);
    }
  }

  @Inject(method = "extractBlockDestroyAnimation", at = @At("HEAD"), cancellable = true)
  private void cancelBlockBreakingProgress(
      Camera camera, LevelRenderState worldRenderState, CallbackInfo ci) {
    if (shouldCancel()) {
      ci.cancel(); // 描画メソッドの実行をキャンセル
    }
  }
}
