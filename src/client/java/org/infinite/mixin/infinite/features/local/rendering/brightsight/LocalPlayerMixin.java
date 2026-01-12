package org.infinite.mixin.infinite.features.local.rendering.brightsight;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.brightsight.BrightSightFeature;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

  public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
    super(clientLevel, gameProfile);
  }

  @Override
  public boolean hasEffect(@NonNull Holder<MobEffect> effect) {
    BrightSightFeature feature =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getBrightSightFeature();
    var unwrapKey = MobEffects.NIGHT_VISION.unwrapKey();
    // BrightSightが有効かつ、対象が暗視効果（Night Vision）であるか確認
    if (feature.isEnabled() && unwrapKey.isPresent() && effect.is(unwrapKey.get())) {
      BrightSightFeature.Method method = feature.getMethod().getValue();

      // UltraBright または NightSight の場合は常に true を返す
      if (method == BrightSightFeature.Method.UltraBright
          || method == BrightSightFeature.Method.NightSight) {
        return true;
      }
    }
    return super.hasEffect(effect);
  }
}
