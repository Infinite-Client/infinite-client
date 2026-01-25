package org.infinite.mixin.infinite.features.global.rendering.theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.global.rendering.theme.ThemeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {
  public EditBoxMixin(int i, int j, int k, int l, Component component) {
    super(i, j, k, l, component);
  }

  @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
  protected void onRenderWidget(
      GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    ThemeFeature theme =
        InfiniteClient.INSTANCE.getGlobalFeatures().getRendering().getThemeFeature();

    if (theme.shouldRenderCustom()) {
      theme
          .getEditBoxRenderer()
          .render((EditBox) (Object) this, guiGraphics, mouseX, mouseY, delta);
      ci.cancel();
    }
  }
}
