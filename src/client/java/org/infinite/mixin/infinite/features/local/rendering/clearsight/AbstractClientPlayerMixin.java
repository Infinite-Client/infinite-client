package org.infinite.mixin.infinite.features.local.rendering.clearsight;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.clearsight.ClearSightFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractClientPlayer.class, priority = 1100)
public abstract class AbstractClientPlayerMixin extends Player {

  public AbstractClientPlayerMixin(Level level, GameProfile gameProfile) {
    super(level, gameProfile);
  }

  @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
  private void onGetFovModifier(CallbackInfoReturnable<Float> cir) {
    ClearSightFeature feature =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getClearSightFeature();

    if (feature.isEnabled() && feature.getAntiFovChange().getValue()) {
      float originalFov = cir.getReturnValue();

      if (originalFov <= 1.0F) {
        cir.setReturnValue(1.0F);
        return;
      }

      double delta = (double) originalFov - 1.0;

      // プロパティから動的に値を取得
      double maxIncrease = feature.getFovMaxIncrease().getValue().doubleValue();
      double intensity = feature.getFovIntensity().getValue().doubleValue();

      // atan を用いたソフトクランプ計算
      double smoothedDelta = (Math.atan(delta * intensity) * (2.0 / Math.PI)) * maxIncrease;

      float finalFov = 1.0F + (float) smoothedDelta;

      cir.setReturnValue(finalFov);
    }
  }
}
