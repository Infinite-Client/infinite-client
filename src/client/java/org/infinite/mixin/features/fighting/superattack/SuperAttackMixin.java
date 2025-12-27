package org.infinite.mixin.features.fighting.superattack;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.infinite.InfiniteClient;
import org.infinite.features.fighting.superattack.SuperAttack;
import org.infinite.features.fighting.superattack.SuperAttack.AttackMethod;
import org.infinite.settings.FeatureSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class SuperAttackMixin {

  @Inject(method = "attack", at = @At("HEAD"))
  private void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
    SuperAttack superAttackFeature = InfiniteClient.INSTANCE.getFeature(SuperAttack.class);

    if (superAttackFeature != null && superAttackFeature.isEnabled()) {
      FeatureSetting.EnumSetting<AttackMethod> methodSetting = superAttackFeature.getMethod();
      AttackMethod method = methodSetting.getValue();
      Minecraft client = Minecraft.getInstance();

      if (client.player != null && client.player.equals(player)) {
        // Conditions from CriticalsHack
        if (!(target instanceof LivingEntity)) return;
        if (!player.onGround()) return;
        if (player.isInWater() || player.isInLava()) return;

        if (method == AttackMethod.FULL_JUMP) {
          // Full Jump (equivalent to CriticalsHack's FULL_JUMP)
          player.jumpFromGround();
        } else if (method == AttackMethod.MINI_JUMP) {
          // Mini Jump (equivalent to CriticalsHack's MINI_JUMP)
          player.push(0.0, 0.1, 0.0);
          player.fallDistance = 0.1F;
          player.setOnGround(false);
        } else if (method == AttackMethod.PACKET) {
          // Packet method (equivalent to CriticalsHack's PACKET)
          sendFakeY(player, 0.0625, true);
          sendFakeY(player, 0, false);
          sendFakeY(player, 1.1e-5, false);
          sendFakeY(player, 0, false);
        }
      }
    }
  }

  @Unique
  private void sendFakeY(Player player, double offset, boolean onGround) {
    Objects.requireNonNull(Minecraft.getInstance().getConnection())
        .send(
            new Pos(
                player.getX(),
                player.getY() + offset,
                player.getZ(),
                onGround,
                player.onGround())); // Use player.isOnGround() for moving argument
  }
}
