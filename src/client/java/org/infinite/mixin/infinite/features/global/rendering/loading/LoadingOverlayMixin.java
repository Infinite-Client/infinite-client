package org.infinite.mixin.infinite.features.global.rendering.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.global.rendering.loading.InfiniteLoadingFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

  @Shadow private float currentProgress;
  @Shadow @Final private boolean fadeIn;
  @Shadow private long fadeInStart;
  @Shadow private long fadeOutStart;

  @Shadow @Final private Minecraft minecraft;

  @Inject(
      method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
      at = @At("TAIL"),
      cancellable = true)
  private void onRender(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
    InfiniteLoadingFeature feature =
        InfiniteClient.INSTANCE.getGlobalFeatures().getRendering().getInfiniteLoadingFeature();

    if (feature.isEnabled()) {
      // --- バニラの計算ロジックをシミュレート ---
      float opacity = getOpacity();

      // 座標計算 (ソースコードの n, p, q, r を再現)
      int centerX = (int) ((double) guiGraphics.guiWidth() * 0.5D);
      int centerY = (int) ((double) guiGraphics.guiHeight() * 0.5D);
      double d = Math.min((double) guiGraphics.guiWidth() * 0.75D, guiGraphics.guiHeight()) * 0.25D;
      int logoHeightHalf = (int) (d * 0.5D);
      double e = d * 4.0D;
      int logoWidthHalf = (int) (e * 0.5D);
      // コンテキストの作成
      InfiniteLoadingFeature.LoadingRenderContext context =
          new InfiniteLoadingFeature.LoadingRenderContext(
              guiGraphics,
              i,
              j,
              f,
              opacity,
              this.currentProgress,
              centerX,
              centerY,
              logoWidthHalf,
              logoHeightHalf);

      // 実行
      feature.handleRender(context);

      // バニラの描画をスキップ
      ci.cancel();
    }
  }

  @Unique
  private float getOpacity() {
    long m = Util.getMillis();
    float g = this.fadeOutStart > -1L ? (float) (m - this.fadeOutStart) / 1000.0F : -1.0F;
    float h = this.fadeInStart > -1L ? (float) (m - this.fadeInStart) / 500.0F : -1.0F;

    float opacity;
    if (g >= 1.0F) {
      opacity = 1.0F - Mth.clamp(g - 1.0F, 0.0F, 1.0F);
    } else if (this.fadeIn) {
      opacity = Mth.clamp(h, 0.0F, 1.0F);
    } else {
      opacity = 1.0F;
    }
    return opacity;
  }
}
