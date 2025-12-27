package org.infinite.mixin.features.server;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.server.connection.AutoConnect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MultiPlayerScreenMixin extends Screen {

  @Unique private Button lastServerButton;

  protected MultiPlayerScreenMixin(Component title) {
    super(title);
  }

  @Unique
  AutoConnect autoConnect() {
    return InfiniteClient.INSTANCE.getFeature(AutoConnect.class);
  }

  @Inject(at = @At("TAIL"), method = "init()V")
  private void onInit(CallbackInfo ci) {
    lastServerButton =
        addRenderableWidget(
            Button.builder(Component.literal("Last Server"), b -> joinLastServer())
                .bounds(width / 2 - 154, 10, 100, 20)
                .build());
    updateLastServerButton();
  }

  @Unique
  private void joinLastServer() {
    AutoConnect autoConnect = autoConnect();
    if (autoConnect != null) autoConnect.joinLastServer((JoinMultiplayerScreen) (Object) this);
  }

  @Inject(at = @At("HEAD"), method = "join(Lnet/minecraft/client/multiplayer/ServerData;)V")
  private void onConnect(ServerData entry, CallbackInfo ci) {
    AutoConnect autoConnect = autoConnect();
    if (autoConnect != null) {
      autoConnect.setLastServer(entry);
      updateLastServerButton();
    }
  }

  @Unique
  private void updateLastServerButton() {
    if (lastServerButton == null) return;

    AutoConnect autoConnect = autoConnect();
    lastServerButton.active = autoConnect != null && autoConnect.getLastServer() != null;
  }
}
