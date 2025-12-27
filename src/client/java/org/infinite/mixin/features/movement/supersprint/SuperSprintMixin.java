package org.infinite.mixin.features.movement.supersprint;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import org.infinite.InfiniteClient;
import org.infinite.features.movement.sprint.SuperSprint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class SuperSprintMixin extends AbstractClientPlayer {

  public SuperSprintMixin(ClientLevel world, GameProfile profile) {
    super(world, profile);
  }

  @WrapOperation(
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z",
              ordinal = 0),
      method = "aiStep()V")
  private boolean wrapHasForwardMovement(ClientInput input, Operation<Boolean> original) {
    // Feature: SuperSprint (Setting: OnlyWhenForward)
    if (InfiniteClient.INSTANCE.isFeatureEnabled(SuperSprint.class)
        && !InfiniteClient.INSTANCE.isSettingEnabled(SuperSprint.class, "OnlyWhenForward"))
      return input.getMoveVector().length() > 1e-5F;

    return original.call(input);
  }

  /** This mixin allows AutoSprint to enable sprinting even when the player is too hungry. */
  @Inject(at = @At("HEAD"), method = "isSprintingPossible", cancellable = true)
  private void onCanSprint(CallbackInfoReturnable<Boolean> cir) {
    // Feature: SuperSprint (Setting: EvenIfHungry)
    if (InfiniteClient.INSTANCE.isSettingEnabled(SuperSprint.class, "EvenIfHungry"))
      cir.setReturnValue(true);
  }
}
