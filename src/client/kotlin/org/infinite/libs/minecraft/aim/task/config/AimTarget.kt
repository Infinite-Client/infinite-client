package org.infinite.libs.minecraft.aim.task.config

import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import org.infinite.libs.graphics.graphics3d.structs.CameraRoll
import org.infinite.libs.interfaces.MinecraftInterface

/**
 * ブロックの狙う面を定義する
 */

sealed class AimTarget : MinecraftInterface() {
    enum class BlockFace {
        Top,
        Bottom,
        North,
        East,
        South,
        West,
        Center,
    }

    open class EntityTarget(
        e: Entity,
    ) : AimTarget() {
        open val entity = e
    }

    // ブロックを狙う面（face）を追加し、デフォルトをCENTERに設定
    open class BlockTarget(
        val blockPos: BlockPos,
        val face: BlockFace = BlockFace.Center, // デフォルトを中央 (CENTER) に設定
    ) : AimTarget() {
        constructor(b: BlockEntity, face: BlockFace = BlockFace.Center) : this(b.blockPos, face)

        fun pos(offset: Double = 0.5): Vec3 {
            val center = blockPos.center
            return when (this.face) {
                BlockFace.Center -> center

                BlockFace.Top -> center.add(0.0, 0.5 * (2 * offset - 1), 0.0)

                // Y+
                BlockFace.Bottom -> center.add(0.0, -0.5 * (2 * offset - 1), 0.0)

                // Y-
                BlockFace.North -> center.add(0.0, 0.0, -0.5 * (2 * offset - 1))

                // Z-
                BlockFace.East -> center.add(0.5 * (2 * offset - 1), 0.0, 0.0)

                // X+
                BlockFace.South -> center.add(0.0, 0.0, 0.5 * (2 * offset - 1))

                // Z+
                BlockFace.West -> center.add(-0.5 * (2 * offset - 1), 0.0, 0.0) // X-
            }
        }
    }

    open class WaypointTarget(
        p: Vec3,
    ) : AimTarget() {
        open val pos = p
    }

    open class RollTarget(
        r: CameraRoll,
    ) : AimTarget() {
        open val roll = r
    }

    fun lookAt() {
        val pos = pos() ?: return
        player?.lookAt(EntityAnchorArgument.Anchor.EYES, pos)
    }

    /**
     * AimTargetのワールド内位置を計算して返します。
     * RollTargetなど、位置を持たない場合は null を返します。
     */
    fun pos(): Vec3? =
        when (this) { // when式の対象を 'this' に変更し、スマートキャストを有効化
            is EntityTarget -> {
                this.entity
                    .getPosition(minecraft.deltaTracker.gameTimeDeltaTicks)
                    .add(0.0, this.entity.getEyeHeight(this.entity.pose).toDouble(), 0.0)
            }

            is BlockTarget -> {
                this.pos()
            }

            is WaypointTarget -> {
                this.pos()
            }

            is RollTarget -> {
                null
            }
        }
}
