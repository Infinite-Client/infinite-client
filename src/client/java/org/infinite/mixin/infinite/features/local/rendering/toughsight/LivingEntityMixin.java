package org.infinite.mixin.infinite.features.local.rendering.toughsight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.toughsight.ToughSightFeature;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

  public LivingEntityMixin(EntityType<?> entityType, Level level) {
    super(entityType, level);
  }

  @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
  private void onHasEffect(@NonNull Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
    LocalPlayer localPlayer = Minecraft.getInstance().player;
    if (localPlayer == null || localPlayer.getId() != this.getId()) {
      return;
    }

    ToughSightFeature toughSight =
        InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getToughSightFeature();
    if (toughSight.isEnabled()) {
      // 盲目
      if (toughSight.getAntiBlindness().getValue() && effect == MobEffects.BLINDNESS) {
        cir.setReturnValue(false);
        return;
      }

      // 暗闇
      if (toughSight.getAntiDarkness().getValue() && effect == MobEffects.DARKNESS) {
        cir.setReturnValue(false);
        return;
      }

      // 吐き気 (NAUSEA)
      if (toughSight.getAntiNausea().getValue() && effect == MobEffects.NAUSEA) {
        cir.setReturnValue(false);
      }
    }
  }
}
