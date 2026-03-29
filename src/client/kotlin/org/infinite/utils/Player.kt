package org.infinite.utils

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult

/**
 * 1.21.1+ 対応: プレイヤーの現在の攻撃射程（槍などの武器属性を含む）に基づき、
 * ターゲットを正確に捉えているかを判定する。
 */
fun Player.isLookingAtEntity(target: Entity): Boolean {
    val attackRange = this.entityInteractionRange()

    val eyePos = this.eyePosition
    val viewVec = this.getViewVector(1.0f)
    val endPos = eyePos.add(viewVec.multiply(attackRange, attackRange, attackRange))

    // ターゲットのBounding Box（当たり判定）を取得。pickRadiusで微調整。
    val collisionBox = target.boundingBox.inflate(target.pickRadius.toDouble())

    // 1. 視線（線分）がエンティティの箱と交差するか
    val hitResultOptional = collisionBox.clip(eyePos, endPos)
    if (!hitResultOptional.isPresent) return false

    val entityHitPos = hitResultOptional.get()
    val distanceToEntity = eyePos.distanceTo(entityHitPos)

    // 2. 距離チェック
    if (distanceToEntity > attackRange) return false

    // 3. 壁越し判定 (Raytrace)
    val blockHit = this.level().clip(
        ClipContext(
            eyePos,
            endPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            this,
        ),
    )

    // ブロックに当たっていない、もしくはブロックより手前にエンティティがいる場合のみ有効
    return blockHit.type == HitResult.Type.MISS || distanceToEntity < eyePos.distanceTo(blockHit.location)
}
