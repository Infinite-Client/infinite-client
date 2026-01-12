package org.infinite.mixin.infinite.features.local.level.esp;

import net.minecraft.world.entity.Entity;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.level.esp.EspFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
  @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
  public void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
    EspFeature espFeature = InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getEspFeature();
    if (espFeature.isEnabled()) {
      cir.setReturnValue(espFeature.isShouldApply((Entity) (Object) this));
    }
  }
}
