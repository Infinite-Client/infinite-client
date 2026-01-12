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
    // 1.21.1+ の属性から射程を取得
    val attackRange = this.entityAttackRange()
    val maxRange = attackRange.maxRange // 槍などの最長到達距離
    val minRange = attackRange.minRange // 最短（近接）の有効距離

    val eyePos = this.eyePosition
    val viewVec = this.getViewVector(1.0f)
    // 最大射程まで視線を伸ばす
    val endPos = eyePos.add(viewVec.x * maxRange, viewVec.y * maxRange, viewVec.z * maxRange)

    // ターゲットのBounding Box（当たり判定）を取得。pickRadiusで微調整。
    val collisionBox = target.boundingBox.inflate(target.pickRadius.toDouble())

    // 1. 視線（線分）がエンティティの箱と交差するか
    val hitResultOptional = collisionBox.clip(eyePos, endPos)
    if (!hitResultOptional.isPresent) return false

    val entityHitPos = hitResultOptional.get()
    val distanceToEntity = eyePos.distanceTo(entityHitPos)

    // 2. 距離チェック (maxRange以内、かつ武器の特性的に有効な距離か)
    // 基本的には maxRange 以内であれば「視線は合っている」とみなせますが、
    // 攻撃の有効判定として使うなら distanceToEntity >= minRange も考慮すべき場合があります。
    if (distanceToEntity !in minRange..maxRange) return false

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
