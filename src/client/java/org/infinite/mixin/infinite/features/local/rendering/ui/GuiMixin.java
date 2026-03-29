package org.infinite.mixin.infinite.features.local.rendering.ui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
  @Unique
  private static UltraUiFeature ultraUiFeature() {
    return InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getUltraUiFeature();
  }

  // クロスヘア
  @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
  private void onRenderCrosshair(
      GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getCrosshairUi().getValue()) {
      ci.cancel();
    }
  }

  // ホットバー (アイテムスロットの並び)
  @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
  private void onRenderHotbar(
      GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getHotbarUi().getValue()) {
      ci.cancel();
    }
  }

  // 経験値レベルの数字描画をキャンセル
  @Inject(
      method = "extractHotbarAndDecorations",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"),
      cancellable = true)
  private void onExtractExperienceLevel(CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getTopBoxUi().getValue()) {
      ci.cancel();
    }
  }

  @Inject(
      method = "extractHotbarAndDecorations",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"),
      cancellable = true)
  private void onRenderExperienceBarBackground(CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getTopBoxUi().getValue()) {
      ci.cancel(); // 厳密にはInvokeのキャンセルは複雑なため、HEADで判定するか下記の方法をとります
    }
  }

  @Inject(
      method = "extractHotbarAndDecorations",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"),
      cancellable = true)
  private void onRenderExperienceBar(CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getTopBoxUi().getValue()) {
      ci.cancel(); // 厳密にはInvokeのキャンセルは複雑なため、HEADで判定するか下記の方法をとります
    }
  }

  /** 左側要素（体力・防御力）の描画を制御 */
  @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
  private void onRenderHealthBar(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getLeftBoxUi().getValue()) {
      ci.cancel();
    }
  }

  @Inject(method = "extractArmor", at = @At("HEAD"), cancellable = true)
  private static void onRenderArmor(
      GuiGraphicsExtractor guiGraphics,
      Player player,
      int i,
      int j,
      int k,
      int l,
      CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getLeftBoxUi().getValue()) {
      ci.cancel();
    }
  }

  /** 右側要素（満腹度）の描画を制御 */
  @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
  private void onRenderFood(
      GuiGraphicsExtractor guiGraphics, Player player, int i, int j, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getRightBoxUi().getValue()) {
      ci.cancel();
    }
  }

  /** 右側要素（空気量/水中呼吸）の描画を制御 */
  @Inject(method = "extractAirBubbles", at = @At("HEAD"), cancellable = true)
  private void onRenderAir(
      GuiGraphicsExtractor guiGraphics, Player player, int i, int j, int k, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getRightBoxUi().getValue()) {
      ci.cancel();
    }
  }

  @Inject(method = "extractVehicleHealth", at = @At("HEAD"), cancellable = true)
  private void onRenderVehicleHealth(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getRightBoxUi().getValue()) {
      ci.cancel();
    }
  }
}
