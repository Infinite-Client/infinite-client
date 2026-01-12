package org.infinite.mixin.infinite.features.local.level.esp;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.level.esp.EspFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

  /**
   * @param entity T型 実体（Entity）
   * @param renderState S型 描画状態保持オブジェクト
   * @param partialTick 部分ティック
   */
  @WrapOperation(
      method =
          "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
  private int onGetTeamColor(
      Entity instance, Operation<Integer> original, T entity, S renderState, float partialTick) {
    EspFeature espFeature = InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getEspFeature();

    if (espFeature.isEnabled()) {
      return espFeature.handleEntityColor(entity, renderState);
    }

    return original.call(instance);
  }

  @Inject(method = "affectedByCulling", at = @At("RETURN"), cancellable = true)
  private void onAffectedByCulling(T entity, CallbackInfoReturnable<Boolean> cir) {
    EspFeature espFeature = InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getEspFeature();
    // ESPが有効な場合は、カリングを無効化して常に描画を試みる
    if (espFeature.isEnabled()) {
      boolean orig = cir.getReturnValue();
      boolean value = espFeature.isShouldApply(entity) || orig;
      cir.setReturnValue(value);
    }
  }

  /** 距離や視界に基づく描画判定。 ここで true を返すと、どんなに遠くにいてもレンダリング対象に含まれます。 */
  @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
  private void onShouldRender(
      T entity,
      Frustum frustum,
      double d,
      double e,
      double f,
      CallbackInfoReturnable<Boolean> cir) {
    EspFeature espFeature = InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getEspFeature();

    if (espFeature.isEnabled()) {
      boolean orig = cir.getReturnValue();
      boolean value = espFeature.isShouldApply(entity) || orig;
      cir.setReturnValue(value);
    }
  }
}
