package org.infinite.mixin.infinite.features.local.rendering.toughsight;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.toughsight.ToughSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

  /** 画面上の様々なエフェクト（ポータル、吐き気など）を描画するメソッドに介入します。 */
  @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
  private void onRenderEffects(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
    ToughSightFeature toughSight =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getToughSightFeature();
    if (toughSight.isEnabled() && toughSight.getAntiNausea().getValue()) {
      ci.cancel();
    }
  }
}
