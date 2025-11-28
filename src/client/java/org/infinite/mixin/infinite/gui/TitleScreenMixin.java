package org.infinite.mixin.infinite.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.infinite.InfiniteClient;
import org.infinite.global.rendering.loading.LoadingAnimationSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
  @Unique private static final Identifier INFINITE_ICON = Identifier.of("infinite", "icon.png");

  @WrapOperation(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V"))
  private void infiniteClient$replacePanorama(
      TitleScreen instance, DrawContext drawContext, float v, Operation<Void> original) {
    if (!InfiniteClient.INSTANCE.isGlobalFeatureEnabled(LoadingAnimationSetting.class)) {
      original.call(instance, drawContext, v);
      return;
    }
    // --- カスタム描画処理（オリジナルのコード） ---
    int width = drawContext.getScaledWindowWidth();
    int height = drawContext.getScaledWindowHeight();

    // ぼかし効果の追加（前回の回答の修正案から追加）
    int blurOverlayColor = 0x40000000;
    drawContext.fill(0, 0, width, height, blurOverlayColor);

    int top = ColorHelper.getArgb(208, 8, 10, 14);
    int bottom = ColorHelper.getArgb(208, 0, 0, 0);
    drawContext.fillGradient(0, 0, width, height, top, bottom);

    int size = Math.min(width, height) / 3;
    size = Math.max(96, Math.min(size, 180));
    int x = (width - size) / 2;
    int y = Math.max(height / 5, (height - size) / 2);
    int logoColor = ColorHelper.getArgb(80, 255, 255, 255);

    drawContext.drawTexture(
        RenderPipelines.GUI_TEXTURED,
        INFINITE_ICON,
        x,
        y,
        0f,
        0f,
        size,
        size,
        256,
        256,
        256,
        256,
        logoColor);
  }
}
