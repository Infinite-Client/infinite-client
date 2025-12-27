package org.infinite.mixin.infinite.graphics;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.infinite.InfiniteClient;
import org.infinite.libs.feature.ConfigurableFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

  // InGameHudのrenderメソッドの描画処理の最後にフック
  @Inject(method = "render", at = @At("TAIL"))
  private void onRenderAtTail(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
    InfiniteClient.INSTANCE.handle2dGraphics(context, tickCounter, ConfigurableFeature.Timing.End);
  }

  @Inject(method = "render", at = @At("HEAD"))
  private void onRenderAtHead(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
    InfiniteClient.INSTANCE.handle2dGraphics(
        context, tickCounter, ConfigurableFeature.Timing.Start);
  }
}
