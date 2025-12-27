package org.infinite.mixin.features.rendering.supersight;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.rendering.sight.SuperSight;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LocalPlayer.class)
public abstract class SuperSightMixin extends AbstractClientPlayer {

  public SuperSightMixin(ClientLevel world, GameProfile profile) {
    super(world, profile);
  }

  public boolean hasEffect(Holder<MobEffect> effect) {
    // Feature: SuperSight

    // NightVision
    if (effect == MobEffects.NIGHT_VISION
        && InfiniteClient.INSTANCE.isSettingEnabled(SuperSight.class, "NightVision")) return true;

    // AntiBlind (BLINDNESS, DARKNESS)
    if (InfiniteClient.INSTANCE.isSettingEnabled(SuperSight.class, "AntiBlind")) {
      if (effect == MobEffects.BLINDNESS || effect == MobEffects.DARKNESS) return false;
    }

    // 💡 修正点: 無限再帰を防ぐため、superを使って元のメソッドを呼び出す
    return super.hasEffect(effect);

    // NOTE: ClientPlayerEntityはabstractではないため、thisを ClientPlayerEntity にキャストして呼び出している可能性があります。
    // より確実な方法は @Redirect または @Overwrite を使用することですが、
    // 現在の構造を維持するなら super を使用してください。
  }
}
