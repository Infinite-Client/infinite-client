package org.infinite.mixin.features.rendering.antioverlay;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.rendering.overlay.AntiOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

  @Inject(
      at = @At("HEAD"),
      method =
          "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V",
      cancellable = true)
  private void onRenderOverlay(
      GuiGraphics context, Identifier texture, float opacity, CallbackInfo ci) {
    if (texture == null) {
      return;
    }

    String path = texture.getPath();

    // 1. カボチャのぼかしオーバーレイのキャンセル
    if ("textures/misc/pumpkinblur.png".equals(path)
        && InfiniteClient.INSTANCE.isSettingEnabled(AntiOverlay.class, "NoPumpkinOverlay")) {
      ci.cancel();
      return;
    }

    // 2. パウダー・スノーのアウトライン/オーバーレイのキャンセル
    // オリジナルコードでは"NoDarknessOverlay"が使われていますが、テクスチャ名から判断して修正を提案します。
    // （"NoDarknessOverlay"が「パウダー・スノーのアウトライン」も制御していると仮定します。）
    if ("textures/misc/powder_snow_outline.png".equals(path)
        && InfiniteClient.INSTANCE.isSettingEnabled(AntiOverlay.class, "NoDarknessOverlay")) {
      ci.cancel();
    }
  }

  @Inject(at = @At("HEAD"), method = "renderVignette", cancellable = true)
  private void onRenderVignetteOverlay(GuiGraphics context, Entity entity, CallbackInfo ci) {

    // ビネット（暗さ）オーバーレイのキャンセル
    // 「NoDarknessOverlay」設定を流用し、ビネットも制御すると仮定します。
    if (InfiniteClient.INSTANCE.isSettingEnabled(AntiOverlay.class, "NoDarknessOverlay")) {
      ci.cancel();
    }
  }
}
