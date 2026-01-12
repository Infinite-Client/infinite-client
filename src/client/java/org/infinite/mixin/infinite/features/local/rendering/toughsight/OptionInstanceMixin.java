package org.infinite.mixin.infinite.features.local.rendering.toughsight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.toughsight.ToughSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionInstance.class)
public class OptionInstanceMixin<T> {

  @SuppressWarnings("unchecked")
  @Inject(method = "get", at = @At("HEAD"), cancellable = true)
  private void onGet(CallbackInfoReturnable<T> cir) {
    ToughSightFeature toughSight =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getToughSightFeature();
    if (!toughSight.isEnabled()) return;
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.screen instanceof OptionsScreen
        || minecraft.screen instanceof AccessibilityOptionsScreen) {
      return;
    }
    Options options = Minecraft.getInstance().options;
    // 1. Darkness Pulse (暗闇の脈動) の偽装
    if (toughSight.getAntiDarkness().getValue()) {
      if ((Object) this == options.darknessEffectScale()) {
        cir.setReturnValue((T) Double.valueOf(0.0));
        return;
      }
    }

    // 2. Screen Effects (吐き気の揺れなど) の偽装
    if (toughSight.getAntiNausea().getValue()) {
      if ((Object) this == options.screenEffectScale()) {
        cir.setReturnValue((T) Double.valueOf(0.0));
      }
    }
  }
}
