package org.infinite.mixin.features.rendering.portalgui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.rendering.portalgui.PortalGui;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class PortalGuiMixin extends AbstractClientPlayer {
  @Shadow(aliases = "client")
  @Final
  protected Minecraft minecraft;

  @Unique private Screen tempCurrentScreen;

  public PortalGuiMixin(ClientLevel world, GameProfile profile) {
    super(world, profile);
  }

  /**
   * PortalGui: When enabled, temporarily sets the current screen to null to prevent the
   * updateNausea() method from closing it.
   */
  @Inject(
      at =
          @At(
              value = "FIELD",
              target =
                  "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;",
              opcode = Opcodes.GETFIELD,
              ordinal = 0),
      method = "handlePortalTransitionEffect(Z)V")
  private void beforeTickNausea(boolean fromPortalEffect, CallbackInfo ci) {
    // Feature: PortalGui の有効性チェック
    if (!InfiniteClient.INSTANCE.isFeatureEnabled(PortalGui.class)) return;

    tempCurrentScreen = minecraft.screen;
    minecraft.screen = null;
  }

  /** PortalGui: Restores the current screen. */
  @Inject(
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/client/player/LocalPlayer;portalEffectIntensity:F",
              opcode = Opcodes.GETFIELD,
              ordinal = 1),
      method = "handlePortalTransitionEffect(Z)V")
  private void afterTickNausea(boolean fromPortalEffect, CallbackInfo ci) {
    if (tempCurrentScreen == null) return;

    minecraft.screen = tempCurrentScreen;
    tempCurrentScreen = null;
  }
}
