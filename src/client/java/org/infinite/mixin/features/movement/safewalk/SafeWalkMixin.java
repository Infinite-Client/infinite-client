package org.infinite.mixin.features.movement.safewalk;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.infinite.InfiniteClient;
import org.infinite.features.movement.walk.SafeWalk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LocalPlayer.class)
public abstract class SafeWalkMixin extends AbstractClientPlayer {

  public SafeWalkMixin(ClientLevel world, GameProfile profile) {
    super(world, profile);
  }

  /** SafeWalk: This is the part that makes SafeWalk work. */
  protected boolean isStayingOnGroundSurface() {
    return super.isStayingOnGroundSurface()
        || InfiniteClient.INSTANCE.isFeatureEnabled(SafeWalk.class);
  }

  /** SafeWalk: Allows SafeWalk to sneak visibly when the player is near a ledge. */
  // NOTE: adjustMovementForSneakingはClientPlayerEntityの親クラスのメソッドをオーバーライドしていると想定
  protected Vec3 maybeBackOffFromEdge(Vec3 movement, MoverType type) {

    Vec3 vec3d = super.maybeBackOffFromEdge(movement, type);
    SafeWalk safeWalk = InfiniteClient.INSTANCE.getFeature(SafeWalk.class);
    if (movement != null
        && InfiniteClient.INSTANCE.isFeatureEnabled(SafeWalk.class)
        && safeWalk != null) {
      safeWalk.onPreMotion();
    }
    return vec3d;
  }
}
