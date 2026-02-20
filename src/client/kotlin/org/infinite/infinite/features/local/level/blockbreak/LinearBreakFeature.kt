package org.infinite.infinite.features.local.level.blockbreak

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.level.LocalLevelCategory
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.graphics.Graphics3D
import kotlin.math.abs

class LinearBreakFeature : LocalFeature() {
    override val featureType = FeatureLevel.Cheat
    override val categoryClass = LocalLevelCategory::class

    val breakRange by property(DoubleProperty(5.0, 1.0, 6.0, " blocks"))
    val swingHand by property(BooleanProperty(true))

    val blocksToMine = LinkedHashSet<BlockPos>()
    var currentBreakingPos: BlockPos? = null
    private var currentBreakingSide: Direction? = null
    var currentBreakingProgress: Float = 0.0f
    private var miningDelayTimer: Int = 0

    fun isWorking(): Boolean = isEnabled() && synchronized(blocksToMine) { blocksToMine.isNotEmpty() }
    val remainingCount: Int get() = synchronized(blocksToMine) { blocksToMine.size }

    fun getProgressPerTick(pos: BlockPos): Float {
        val player = player ?: return 0f
        val world = level ?: return 0f
        val state = world.getBlockState(pos)
        val speed = player.getDestroySpeed(state)
        val hardness = state.getDestroySpeed(world, pos)
        return if (hardness > 0) speed / (hardness * (if (player.hasCorrectToolForDrops(state)) 30f else 100f)) else 1.0f
    }

    override fun onStartTick() {
        val player = player ?: return
        if (options.keyAttack.isDown) {
            val hit = minecraft.hitResult
            if (hit != null && hit.type == HitResult.Type.BLOCK) tryAdd((hit as BlockHitResult).blockPos)
        }
        val rangeSq = breakRange.value * breakRange.value
        synchronized(blocksToMine) { blocksToMine.retainAll { it.distSqr(player.blockPosition()) <= rangeSq } }
        if (miningDelayTimer > 0) {
            miningDelayTimer--
            return
        }
        minecraft.execute { mine() }
    }

    fun tryAdd(pos: BlockPos): Boolean {
        val lvl = level ?: return false
        val p = player ?: return false
        val state = lvl.getBlockState(pos)
        if (state.isAir || !state.fluidState.isEmpty || state.getDestroySpeed(lvl, pos) < 0) return false
        if (getProgressPerTick(pos) >= 1.0f) return false
        val veinBreak = InfiniteClient.localFeatures.level.veinBreakFeature
        if (veinBreak.isEnabled() && veinBreak.isOreBlock(state.block)) return false
        synchronized(blocksToMine) { return blocksToMine.add(pos) }
    }

    private fun mine() {
        val world = level ?: return
        val player = player ?: return
        val fastBreak = InfiniteClient.localFeatures.level.fastBreakFeature
        val rangeSq = breakRange.value * breakRange.value

        val targetPos = synchronized(blocksToMine) { blocksToMine.firstOrNull() } ?: run {
            if (currentBreakingPos != null) {
                sendAbortPacket(currentBreakingPos!!, currentBreakingSide ?: Direction.UP)
                resetCurrentState()
            }
            return
        }

        val blockState = world.getBlockState(targetPos)
        if (blockState.isAir || targetPos.distSqr(player.blockPosition()) > rangeSq) {
            synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
            if (currentBreakingPos == targetPos) resetCurrentState()
            return
        }

        if (currentBreakingPos == null || currentBreakingPos != targetPos) {
            if (currentBreakingPos != null) sendAbortPacket(currentBreakingPos!!, currentBreakingSide ?: Direction.UP)
            currentBreakingPos = targetPos
            currentBreakingSide = getSide(targetPos) ?: Direction.UP
            currentBreakingProgress = 0.0f
            sendStartPacket(targetPos, currentBreakingSide!!)
        } else {
            currentBreakingProgress += getProgressPerTick(targetPos)
            val shouldFinish = currentBreakingProgress >= 1.0f ||
                (fastBreak.isEnabled() && !fastBreak.safeMode.value && fastBreak.shouldFastBreak(targetPos, currentBreakingProgress))

            if (shouldFinish) {
                fastBreak.sendStopPacket(targetPos, currentBreakingSide!!)

                // 強力なクライアント同期 (ゴーストブロック防止)
                world.levelEvent(2001, targetPos, Block.getId(blockState))
                world.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3)

                synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
                resetCurrentState()
                miningDelayTimer = if (fastBreak.isEnabled()) fastBreak.interval.value else 5
            }
        }
        if (swingHand.value && currentBreakingPos != null) player.swing(InteractionHand.MAIN_HAND)
    }

    private fun getSide(pos: BlockPos): Direction? {
        val hit = level?.clip(ClipContext(player!!.eyePosition, Vec3.atCenterOf(pos), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player!!))
        return if (hit != null && hit.type == HitResult.Type.BLOCK && hit.blockPos == pos) hit.direction else null
    }

    private fun sendStartPacket(pos: BlockPos, side: Direction) {
        connection?.send(ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, side))
    }

    private fun sendAbortPacket(pos: BlockPos, side: Direction) {
        connection?.send(ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, side))
    }

    private fun resetCurrentState() {
        currentBreakingPos = null
        currentBreakingSide = null
        currentBreakingProgress = 0.0f
    }

    override fun onLevelRendering(graphics3D: Graphics3D) {
        val color = InfiniteClient.theme.colorScheme.accentColor
        val list = synchronized(blocksToMine) { blocksToMine.toList() }
        list.forEach { pos -> graphics3D.boxOptimized(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()), Vec3(pos.x + 1.0, pos.y + 1.0, pos.z + 1.0), color, 1.0f, true) }
        currentBreakingPos?.let { pos ->
            val p = currentBreakingProgress.coerceIn(0f, 1f)
            val offset = (1.0 - p) * 0.5
            val min = Vec3(pos.x + offset, pos.y + offset, pos.z + offset).add(0.005, 0.005, 0.005)
            val max = Vec3(pos.x + 1.0 - offset, pos.y + 1.0 - offset, pos.z + 1.0 - offset).subtract(0.005, 0.005, 0.005)
            renderSolidBox(graphics3D, min, max, (color and 0x00FFFFFF) or 0x80000000.toInt())
        }
    }

    private fun renderSolidBox(g: Graphics3D, min: Vec3, max: Vec3, color: Int) {
        val x0 = min.x
        val y0 = min.y
        val z0 = min.z
        val x1 = max.x
        val y1 = max.y
        val z1 = max.z
        g.rectangleFill(Vec3(x0, y0, z1), Vec3(x1, y0, z1), Vec3(x1, y0, z0), Vec3(x0, y0, z0), color, false)
        g.rectangleFill(Vec3(x0, y1, z0), Vec3(x1, y1, z0), Vec3(x1, y1, z1), Vec3(x0, y1, z1), color, false)
        g.rectangleFill(Vec3(x1, y0, z0), Vec3(x1, y1, z0), Vec3(x0, y1, z0), Vec3(x0, y0, z0), color, false)
        g.rectangleFill(Vec3(x0, y0, z1), Vec3(x0, y1, z1), Vec3(x1, y1, z1), Vec3(x1, y0, z1), color, false)
        g.rectangleFill(Vec3(x0, y0, z0), Vec3(x0, y1, z0), Vec3(x0, y1, z1), Vec3(x0, y0, z1), color, false)
        g.rectangleFill(Vec3(x1, y0, z1), Vec3(x1, y1, z1), Vec3(x1, y1, z0), Vec3(x1, y0, z0), color, false)
    }

    override fun onEnabled() {
        synchronized(blocksToMine) { blocksToMine.clear() }
        resetCurrentState()
    }
    override fun onDisabled() {
        if (currentBreakingPos != null) sendAbortPacket(currentBreakingPos!!, currentBreakingSide ?: Direction.UP)
        synchronized(blocksToMine) { blocksToMine.clear() }
        resetCurrentState()
    }
}
