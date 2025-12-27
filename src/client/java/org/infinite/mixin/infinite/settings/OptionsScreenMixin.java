package org.infinite.mixin.infinite.settings;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.infinite.gui.screen.GlobalSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraftのオプション画面 (OptionsScreen) にカスタム設定ボタンを追加するためのMixin。
 * このボタンは、既存のボタン群と同じGridWidget.Adderを使って追加され、レイアウトに統合されます。
 */
@Environment(EnvType.CLIENT)
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
  private OptionsScreenMixin(Component title) {
    super(title);
  }

  @Unique
  private Button createButton(Component message, Supplier<Screen> screenSupplier) {
    return Button.builder(
            message,
            (button) -> {
              if (this.minecraft != null) {
                this.minecraft.setScreen(screenSupplier.get());
              }
            })
        .build();
  }

  /**
   * OptionsScreenのinit()メソッドにコードを注入し、既存のGridWidget.Adderを使用して カスタム設定ボタンをオプションリストに追加します。 注入ポイント:
   * GridWidgetがボディに追加される直前。 LocalCapture: ローカル変数として定義されたGridWidget.Adderをキャプチャします。
   *
   * @param ci コールバック情報
   * @param adder init()内で定義されたローカル変数 (GridWidget.Adder)
   */
  @Inject(
      method = "init()V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
  private void infinite$addModConfigButtonInGrid(
      CallbackInfo ci, @Local GridLayout.RowHelper adder) {
    adder.addChild(
        this.createButton(
            Component.literal("Infinite Client Settings"),
            () -> new GlobalSettingsScreen((OptionsScreen) (Object) this)));
  }
}
