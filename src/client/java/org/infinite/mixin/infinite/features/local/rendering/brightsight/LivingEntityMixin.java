package org.infinite.mixin.infinite.features.local.rendering.brightsight;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.rendering.brightsight.BrightSightFeature;
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
      BrightSightFeature brightSight =
          InfiniteClient.INSTANCE.getLocalFeatures().getRendering().getBrightSightFeature();
      if (brightSight.isEnabled()) {
        var nightVisionKey = MobEffects.NIGHT_VISION.unwrapKey();
        if (nightVisionKey.isPresent() && effect.is(nightVisionKey.get())) {
          BrightSightFeature.Method method = brightSight.getMethod().getValue();
          if (method == BrightSightFeature.Method.UltraBright
              || method == BrightSightFeature.Method.NightSight) {
            cir.setReturnValue(true);
          }
        }
      }
    }
  }
}
