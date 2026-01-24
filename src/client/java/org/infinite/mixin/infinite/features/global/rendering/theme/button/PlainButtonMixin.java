// PlainButtonMixin.java
package org.infinite.mixin.infinite.features.global.rendering.theme.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.network.chat.Component;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.global.rendering.theme.ThemeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlainTextButton.class) // Button.Plain は通常 Button を継承した無名/内部クラス扱い
public abstract class PlainButtonMixin extends Button {

  protected PlainButtonMixin(
      int i,
      int j,
      int k,
      int l,
      Component component,
      OnPress onPress,
      CreateNarration createNarration) {
    super(i, j, k, l, component, onPress, createNarration);
  }

  @Inject(method = "renderContents", at = @At("HEAD"), cancellable = true)
  public void onRenderWidget(
      GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    ThemeFeature theme =
        InfiniteClient.INSTANCE.getGlobalFeatures().getRendering().getThemeFeature();

    // Button.Plain かどうかのチェック（すべての Button に適用したくない場合）
    if (theme.shouldRenderCustom()) {
      theme.getPlainButtonRenderer().render(this, guiGraphics, mouseX, mouseY, delta);
      ci.cancel();
    }
  }
}
