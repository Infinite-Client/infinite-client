package org.infinite.mixin.features.rendering.freecamera;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.infinite.InfiniteClient;
import org.infinite.features.rendering.camera.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ZeroHitBox {

  // 衝突ボックスを返すメソッドをフックする
  @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
  private void zeroBoundingBox(CallbackInfoReturnable<AABB> cir) {
    if (!InfiniteClient.INSTANCE.isFeatureEnabled(FreeCamera.class)) return;
    Entity entity = (Entity) (Object) this;
    if (entity instanceof Player) {
      AABB zeroBox = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      cir.setReturnValue(zeroBox);
      cir.cancel();
    }
  }
}
