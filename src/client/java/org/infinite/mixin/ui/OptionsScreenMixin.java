package org.infinite.mixin.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.infinite.InfiniteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

  protected OptionsScreenMixin(Component title) {
    super(title);
  }

  // クレジットボタンが追加された後のタイミングで rowHelper をキャプチャ
  @Inject(
      method = "init",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/screens/options/OptionsScreen;openScreenButton(Lnet/minecraft/network/chat/Component;Ljava/util/function/Supplier;)Lnet/minecraft/client/gui/components/Button;",
              ordinal = 9 // 何番目の openScreenButton 呼び出しの後かを指定（クレジットは最後の方）
              ),
      locals = LocalCapture.CAPTURE_FAILSOFT)
  private void onInit(
      CallbackInfo ci,
      net.minecraft.client.gui.layouts.LinearLayout linearLayout,
      net.minecraft.client.gui.layouts.LinearLayout linearLayout2,
      GridLayout gridLayout,
      GridLayout.RowHelper rowHelper) {

    rowHelper.addChild(
        Button.builder(
                Component.literal("Infinite Settings"),
                (button) -> {
                  Screen parent = minecraft.screen;
                  InfiniteClient.INSTANCE.handleOpenGlobalSettingScreen(parent);
                })
            .build());
  }
}
