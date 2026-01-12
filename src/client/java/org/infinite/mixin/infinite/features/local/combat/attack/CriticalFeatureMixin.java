package org.infinite.mixin.infinite.features.local.combat.attack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.infinite.InfiniteClient;
import org.infinite.infinite.features.local.combat.attack.CriticalFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class CriticalFeatureMixin {

  @Inject(method = "attack", at = @At("HEAD"))
  private void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
    CriticalFeature criticalFeature =
        InfiniteClient.INSTANCE.getLocalFeatures().getCombat().getCriticalFeature();

    if (criticalFeature.isEnabled()) {
      Minecraft mc = Minecraft.getInstance();

      // プレイヤー自身による攻撃かつ対象が生存エンティティであるか確認
      if (mc.player != null && mc.player.equals(player)) {
        if (!(target instanceof LivingEntity)) return;

        // クリティカルが発生しない条件のチェック
        if (!player.onGround()) return;
        if (player.isInWater() || player.isInLava() || player.isPassenger()) return;

        CriticalFeature.CriticalMode mode = criticalFeature.getMode().getValue();

        if (mode == CriticalFeature.CriticalMode.FullJump) {
          player.jumpFromGround();
        } else if (mode == CriticalFeature.CriticalMode.MiniJump) {
          // クライアント側で一瞬浮いた判定にする
          player.push(0, 0.1, 0);
          player.fallDistance = 0.1F;
          player.setOnGround(false);
        } else if (mode == CriticalFeature.CriticalMode.Packet) {
          // パケットによる偽装
          sendFakeY(player, 0.0625);
          sendFakeY(player, 0.0);
          sendFakeY(player, 1.1E-5);
          sendFakeY(player, 0.0);
        }
      }
    }
  }

  @Unique
  private void sendFakeY(Player player, double offset) {
    ClientPacketListener connection = Minecraft.getInstance().getConnection();
    if (connection != null) {
      // ServerboundMovePlayerPacket.Pos を使用
      connection.send(
          new ServerboundMovePlayerPacket.Pos(
              player.getX(),
              player.getY() + offset,
              player.getZ(),
              false,
              false // horizontalCollision
              ));
    }
  }
}
