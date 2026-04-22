package org.infinite.mixin.infinite.features.local.level.xray;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import org.infinite.InfiniteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateBase.class)
public abstract class BlockStateBaseMixin {

  /**
   * ブロック自体の影（Shade）の計算を上書きします。 これにより、X-Ray有効時に地中の鉱石などが暗く沈むのを防ぎます。 1.0Fを返すことで、常に最大輝度のシェーディングになります。
   */
  @Inject(method = "getShadeBrightness", at = @At("RETURN"), cancellable = true)
  private void onGetShadeBrightness(
      BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
    if (InfiniteClient.INSTANCE.getLocalFeatures().getLevel().getXRayFeature().isEnabled()) {
      // 影をなくし、フラットな明るさにする
      cir.setReturnValue(1.0F);
    }
  }
}
