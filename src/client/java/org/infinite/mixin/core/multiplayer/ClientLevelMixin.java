package org.infinite.mixin.core.multiplayer;

import java.util.function.BooleanSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.infinite.InfiniteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
@SuppressWarnings("Unused")
public abstract class ClientLevelMixin extends Level {

  protected ClientLevelMixin(
      WritableLevelData writableLevelData,
      ResourceKey<Level> resourceKey,
      RegistryAccess registryAccess,
      Holder<DimensionType> holder,
      boolean bl,
      boolean bl2,
      long l,
      int i) {
    super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
  }

  @Inject(at = @At("HEAD"), method = "tick")
  private void onStartTick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
    InfiniteClient.INSTANCE.getWorldTicks().onStartTick();
  }

  @Inject(at = @At("TAIL"), method = "tick")
  private void onEndTick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
    InfiniteClient.INSTANCE.getWorldTicks().onEndTick();
  }
}
