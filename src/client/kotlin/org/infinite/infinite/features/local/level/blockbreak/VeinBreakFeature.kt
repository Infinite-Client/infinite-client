package org.infinite.infinite.features.local.level.blockbreak

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
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
import org.infinite.libs.core.features.property.list.BlockListProperty
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics3D
import java.util.*
import kotlin.math.abs

class VeinBreakFeature : LocalFeature() {
    override val featureType = FeatureLevel.Cheat
    override val categoryClass = LocalLevelCategory::class

    val blockList by property(BlockListProperty(listOf("minecraft:ancient_debris", "minecraft:coal_ore", "minecraft:copper_ore", "minecraft:deepslate_coal_ore", "minecraft:deepslate_copper_ore", "minecraft:deepslate_diamond_ore", "minecraft:deepslate_emerald_ore", "minecraft:deepslate_gold_ore", "minecraft:deepslate_iron_ore", "minecraft:deepslate_lapis_ore", "minecraft:deepslate_redstone_ore", "minecraft:diamond_ore", "minecraft:emerald_ore", "minecraft:gold_ore", "minecraft:iron_ore", "minecraft:lapis_ore", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore", "minecraft:redstone_ore")))
    val breakRange by property(DoubleProperty(5.0, 1.0, 6.0, " blocks"))
    val maxBlocks by property(IntProperty(64, 1, 500))
    val swingHand by property(BooleanProperty(true))

    private val blocksToMine = LinkedHashSet<BlockPos>()
    private var currentBreakingPos: BlockPos? = null
    private var currentBreakingSide: Direction? = null
    private var currentBreakingProgress: Float = 0.0f
    private var miningDelayTimer: Int = 0

    fun isWorking(): Boolean = isEnabled() && synchronized(blocksToMine) { blocksToMine.isNotEmpty() }
    val remainingCount: Int get() = synchronized(blocksToMine) { blocksToMine.size }

    override fun onStartTick() {
        val player = player ?: return
        if (options.keyAttack.isDown) {
            val hit = minecraft.hitResult
            if (hit != null && hit.type == HitResult.Type.BLOCK) tryAdd((hit as BlockHitResult).blockPos)
        }
        synchronized(blocksToMine) { blocksToMine.retainAll { it.distSqr(player.blockPosition()) <= (breakRange.value * breakRange.value) } }
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
        if (state.isAir || !state.fluidState.isEmpty || !isOreBlock(state.block) || state.getDestroySpeed(lvl, pos) < 0) return false
        synchronized(blocksToMine) {
            if (blocksToMine.isEmpty() || currentBreakingPos == null) findVein(pos) else blocksToMine.add(pos)
            return blocksToMine.contains(pos)
        }
    }

    fun isOreBlock(block: Block): Boolean = blockList.value.contains(BuiltInRegistries.BLOCK.getKey(block).toString())

    private fun findVein(startPos: BlockPos) {
        val world = level ?: return
        val queue: Queue<BlockPos> = LinkedList()
        val visited = mutableSetOf<BlockPos>()
        queue.offer(startPos)
        visited.add(startPos)
        val rangeSq = breakRange.value * breakRange.value
        while (queue.isNotEmpty() && blocksToMine.size < maxBlocks.value) {
            val currentPos = queue.poll() ?: continue
            val block = world.getBlockState(currentPos).block
            if (isOreBlock(block)) {
                blocksToMine.add(currentPos)
                for (direction in Direction.entries) {
                    val neighborPos = currentPos.relative(direction)
                    if (!visited.contains(neighborPos) && neighborPos.distSqr(startPos) <= rangeSq) {
                        visited.add(neighborPos)
                        queue.offer(neighborPos)
                    }
                }
            }
        }
    }

    private fun mine() {
        val world = level ?: return
        val player = player ?: return
        val fastBreak = InfiniteClient.localFeatures.level.fastBreakFeature
        val targetPos = synchronized(blocksToMine) { blocksToMine.firstOrNull() } ?: run {
            if (currentBreakingPos != null) {
                sendAbortPacket(currentBreakingPos!!, currentBreakingSide ?: Direction.UP)
                resetCurrentState()
            }
            return
        }
        val blockState = world.getBlockState(targetPos)
        if (blockState.isAir || !isOreBlock(blockState.block) || targetPos.distSqr(player.blockPosition()) > breakRange.value * breakRange.value) {
            synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
            if (currentBreakingPos == targetPos) {
                sendAbortPacket(targetPos, currentBreakingSide ?: Direction.UP)
                resetCurrentState()
            }
            return
        }
        if (currentBreakingPos == null || currentBreakingPos != targetPos) {
            if (currentBreakingPos != null) sendAbortPacket(currentBreakingPos!!, currentBreakingSide ?: Direction.UP)
            currentBreakingPos = targetPos
            currentBreakingSide = getSide(targetPos) ?: Direction.UP
            currentBreakingProgress = 0.0f
            sendStartPacket(targetPos, currentBreakingSide!!)
        } else {
            val speed = player.getDestroySpeed(blockState)
            val hardness = blockState.getDestroySpeed(world, targetPos)
            val pPerTick = if (hardness > 0) speed / (hardness * (if (player.hasCorrectToolForDrops(blockState)) 30f else 100f)) else 1.0f
            currentBreakingProgress += pPerTick
            if (currentBreakingProgress >= 1.0f || (fastBreak.isEnabled() && !fastBreak.safeMode.value && fastBreak.shouldFastBreak(targetPos, currentBreakingProgress))) {
                fastBreak.sendStopPacket(targetPos, currentBreakingSide!!)
                world.levelEvent(2001, targetPos, Block.getId(blockState))
                world.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3)
                synchronized(blocksToMine) {
                    blocksToMine.remove(targetPos)
                    findVein(targetPos)
                }
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

    private fun sendStartPacket(pos: BlockPos, side: Direction) = connection?.send(ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, side))
    private fun sendAbortPacket(pos: BlockPos, side: Direction) = connection?.send(ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, side))
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
