package org.infinite.mixin.infinite.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.infinite.InfiniteClient;
import org.infinite.libs.global.rendering.loading.LoadingAnimationSetting;
import org.infinite.libs.global.rendering.theme.overlay.InfiniteLoadingScreenRenderer;
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
  @Shadow private long fadeOutStart;
  @Shadow private long fadeInStart;
  @Shadow @Final private boolean fadeIn;

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
      GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    if (shouldInject()) {
      // 画面全体を黒 (0xFF000000) で塗りつぶします。
      // Alpha: 255 (FF), Red: 00, Green: 00, Blue: 00
      int blackColor = ARGB.color(255, 0, 0, 0);
      context.fill(0, 0, context.guiWidth(), context.guiHeight(), blackColor);
    }
  }

  // --- 既存のカスタム描画 (TAIL) ---

  @Inject(method = "render", at = @At("TAIL"))
  private void infiniteClient$renderCustomOverlay(
      GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    if (shouldInject()) {
      // カスタム描画を実行します。
      InfiniteLoadingScreenRenderer.render(
          context, this.currentProgress, this.fadeInStart, this.fadeOutStart, this.fadeIn);
    }
  }

  // --- 既存の WrapOperation (抑制) ---

  /** Mojangの背景塗りつぶし (DrawContext.fill) を抑制します。 これを抑制しないと、上で強制した黒背景の上に元のグラデーションが描画されてしまいます。 */
  @WrapOperation(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
  private void infiniteClient$wrapVanillaFill(
      GuiGraphics instance, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
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
                  "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
  private void infiniteClient$wrapVanillaTexture(
      GuiGraphics instance,
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
                  "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"))
  private void infiniteClient$wrapVanillaProgressBar(
      LoadingOverlay instance,
      GuiGraphics context,
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
