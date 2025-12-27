package org.infinite.mixin.features.rendering.freecamera;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.rendering.camera.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin extends AbstractClientPlayer {
  public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
    super(world, profile);
  }

  @Override
  public boolean isSpectator() {
    if (InfiniteClient.INSTANCE.isFeatureEnabled(FreeCamera.class)) {
      return true;
    } else {
      return super.isSpectator();
    }
  }
}
