package org.infinite.mixin.graphics;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.infinite.libs.core.tick.RenderTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class UiMixin {

  @Inject(method = "render", at = @At("HEAD"))
  public void onRenderHead(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    RenderTicks.INSTANCE.onStartUiRendering(guiGraphics, deltaTracker);
  }

  @Inject(method = "render", at = @At("TAIL"))
  public void onRenderTail(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    RenderTicks.INSTANCE.onEndUiRendering(guiGraphics, deltaTracker);
  }
}
