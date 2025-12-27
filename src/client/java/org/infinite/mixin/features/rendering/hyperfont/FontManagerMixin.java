package org.infinite.mixin.features.rendering.hyperfont;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontManager;
import org.infinite.infinite.features.rendering.font.HyperTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontManager.class)
public abstract class FontManagerMixin {
  @Inject(
      at = @At("HEAD"),
      method = "createFont()Lnet/minecraft/client/gui/Font;",
      cancellable = true)
  public void onCreateTextRenderer(CallbackInfoReturnable<Font> cir) {
    cir.setReturnValue(new HyperTextRenderer((FontManager) (Object) this));
  }
}
