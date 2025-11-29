package org.infinite.mixin.infinite.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.infinite.InfiniteClient;
import org.infinite.global.rendering.loading.LoadingAnimationSetting;
import org.infinite.global.rendering.theme.overlay.InfiniteLoadingScreenRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
  @Shadow private float progress;
  @Shadow private long reloadCompleteTime;
  @Shadow private long reloadStartTime;
  @Shadow @Final private boolean reloading;

  @Unique
  private boolean shouldInject() {
    return InfiniteClient.INSTANCE.isGlobalFeatureEnabled(LoadingAnimationSetting.class);
  }

  // --- 追加: モノクロ背景の強制 ---

  /**
   * renderメソッドのHEADに介入し、shouldInject()がtrueの場合、画面を黒一色で塗りつぶします。
   * これにより、元のグラデーションやクリアカラーが上書きされ、モノクロ背景が強制されます。
   */
  @Inject(method = "render", at = @At("HEAD"))
  private void infiniteClient$forceMonochromeBackground(
      DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    if (shouldInject()) {
      // 画面全体を黒 (0xFF000000) で塗りつぶします。
      // Alpha: 255 (FF), Red: 00, Green: 00, Blue: 00
      int blackColor = ColorHelper.getArgb(255, 0, 0, 0);
      context.fill(
          0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), blackColor);
    }
  }

  // --- 既存のカスタム描画 (TAIL) ---

  @Inject(method = "render", at = @At("TAIL"))
  private void infiniteClient$renderCustomOverlay(
      DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    if (shouldInject()) {
      // カスタム描画を実行します。
      InfiniteLoadingScreenRenderer.render(
          context, this.progress, this.reloadStartTime, this.reloadCompleteTime, this.reloading);
    }
  }

  // --- 既存の WrapOperation (抑制) ---

  /** Mojangの背景塗りつぶし (DrawContext.fill) を抑制します。 これを抑制しないと、上で強制した黒背景の上に元のグラデーションが描画されてしまいます。 */
  @WrapOperation(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
  private void infiniteClient$wrapVanillaFill(
      DrawContext instance, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    if (!shouldInject()) {
      // カスタム機能が無効な場合のみ、元の fill 処理を実行
      original.call(instance, x1, y1, x2, y2, color);
    }
  }

  /** Mojangロゴのテクスチャ描画 (DrawContext.drawTexture) を抑制します。 */
  @WrapOperation(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"))
  private void infiniteClient$wrapVanillaTexture(
      DrawContext instance,
      RenderPipeline pipeline,
      Identifier sprite,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      int color,
      Operation<Void> original) {
    if (!shouldInject()) {
      // カスタム機能が無効な場合のみ、元の drawTexture 処理を実行
      original.call(
          instance,
          pipeline,
          sprite,
          x,
          y,
          u,
          v,
          width,
          height,
          regionWidth,
          regionHeight,
          textureWidth,
          textureHeight,
          color);
    }
  }

  /** Mojangのプログレスバー描画 (SplashOverlay.renderProgressBar) を抑制します。 */
  @WrapOperation(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/screen/SplashOverlay;renderProgressBar(Lnet/minecraft/client/gui/DrawContext;IIIIF)V"))
  private void infiniteClient$wrapVanillaProgressBar(
      SplashOverlay instance,
      DrawContext context,
      int minX,
      int minY,
      int maxX,
      int maxY,
      float opacity,
      Operation<Void> original) {
    if (!shouldInject()) {
      original.call(instance, context, minX, minY, maxX, maxY, opacity);
    }
  }
}
