package org.infinite.mixin.infinite.features.local.rendering.ui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
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
  @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
  private void onRenderCrosshair(
      GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getCrosshairUi().getValue()) {
      ci.cancel();
    }
  }

  // ホットバー (アイテムスロットの並び)
  @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
  private void onRenderHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getHotbarUi().getValue()) {
      ci.cancel();
    }
  }

  /** 左側要素（体力・防御力）の描画を制御 */
  @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
  private void onRenderHealthBar(GuiGraphics guiGraphics, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getLeftBoxUi().getValue()) {
      ci.cancel();
    }
  }

  @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
  private static void onRenderArmor(
      GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getLeftBoxUi().getValue()) {
      ci.cancel();
    }
  }

  /** 右側要素（満腹度）の描画を制御 */
  @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
  private void onRenderFood(GuiGraphics guiGraphics, Player player, int i, int j, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getRightBoxUi().getValue()) {
      ci.cancel();
    }
  }

  /** 右側要素（空気量/水中呼吸）の描画を制御 */
  @Inject(method = "renderAirBubbles", at = @At("HEAD"), cancellable = true)
  private void onRenderAir(
      GuiGraphics guiGraphics, Player player, int i, int j, int k, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getRightBoxUi().getValue()) {
      ci.cancel();
    }
  }

  @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
  private void onRenderVehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
    if (ultraUiFeature().isEnabled() && ultraUiFeature().getRightBoxUi().getValue()) {
      ci.cancel();
    }
  }
}
