package org.infinite.mixin.infinite.features.local.rendering.toughsight;

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
    if ((Object) this instanceof LocalPlayer) {
      ToughSightFeature toughSight =
          InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getToughSightFeature();
      if (toughSight.isEnabled()) {
        // 盲目
        var blindnessKey = MobEffects.BLINDNESS.unwrapKey();
        if (toughSight.getAntiBlindness().getValue()
            && blindnessKey.isPresent()
            && effect.is(blindnessKey.get())) {
          cir.setReturnValue(false);
          return;
        }

        // 暗闇
        var darknessKey = MobEffects.DARKNESS.unwrapKey();
        if (toughSight.getAntiDarkness().getValue()
            && darknessKey.isPresent()
            && effect.is(darknessKey.get())) {
          cir.setReturnValue(false);
          return;
        }

        // 吐き気 (NAUSEA)
        var nauseaKey = MobEffects.NAUSEA.unwrapKey();
        if (toughSight.getAntiNausea().getValue()
            && nauseaKey.isPresent()
            && effect.is(nauseaKey.get())) {
          cir.setReturnValue(false);
        }
      }
    }
  }
}
