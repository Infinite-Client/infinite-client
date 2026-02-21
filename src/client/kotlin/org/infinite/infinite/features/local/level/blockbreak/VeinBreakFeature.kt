package org.infinite.infinite.features.local.level.blockbreak

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.level.LocalLevelCategory
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.list.BlockListProperty
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics3D
import java.util.*

class VeinBreakFeature : LocalFeature() {
    override val featureType = FeatureLevel.Cheat
    override val categoryClass = LocalLevelCategory::class

    val blockList by property(BlockListProperty(listOf("minecraft:ancient_debris", "minecraft:coal_ore", "minecraft:copper_ore", "minecraft:deepslate_coal_ore", "minecraft:deepslate_copper_ore", "minecraft:deepslate_diamond_ore", "minecraft:deepslate_emerald_ore", "minecraft:deepslate_gold_ore", "minecraft:deepslate_iron_ore", "minecraft:deepslate_lapis_ore", "minecraft:deepslate_redstone_ore", "minecraft:diamond_ore", "minecraft:emerald_ore", "minecraft:gold_ore", "minecraft:iron_ore", "minecraft:lapis_ore", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore", "minecraft:redstone_ore")))
    val breakRange by property(DoubleProperty(5.0, 1.0, 6.0, " blocks"))
    val maxBlocks by property(IntProperty(64, 1, 500))

    val blocksToMine = LinkedHashSet<BlockPos>()
    var currentBreakingPos: BlockPos? = null
    private var currentBreakingSide: Direction? = null
    var currentBreakingProgress: Float = 0.0f
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
        val rangeSq = breakRange.value * breakRange.value

        val targetPos = synchronized(blocksToMine) { blocksToMine.firstOrNull() } ?: run {
            if (currentBreakingPos != null) {
                resetCurrentState()
            }
            return
        }

        val blockState = world.getBlockState(targetPos)
        if (blockState.isAir) {
            synchronized(blocksToMine) {
                blocksToMine.remove(targetPos)
                findVein(targetPos)
            }
            if (currentBreakingPos == targetPos) {
                resetCurrentState()
                miningDelayTimer = 5
            }
            return
        }

        if (targetPos.distSqr(player.blockPosition()) > rangeSq || !isOreBlock(blockState.block)) {
            synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
            if (currentBreakingPos == targetPos) {
                resetCurrentState()
            }
            return
        }

        val side = LinearBreakFeature.getSide(minecraft, targetPos)

        // クリエイティブの場合
        if (player.abilities.instabuild) {
            LinearBreakFeature.instantBreak(minecraft, targetPos, side)
            synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
            miningDelayTimer = 1
            return
        }

        if (currentBreakingPos != targetPos) {
            currentBreakingPos = targetPos
            currentBreakingSide = side
            currentBreakingProgress = 0.0f
            LinearBreakFeature.startBreaking(minecraft, targetPos, side)
        } else {
            // 自前で進捗を計算 (LinearBreakFeatureと同様のロジック)
            currentBreakingProgress += LinearBreakFeature.getProgressPerTick(minecraft, targetPos)

            // 腕振り
            if (minecraft.level!!.gameTime % 2 == 0L) {
                player.swing(InteractionHand.MAIN_HAND)
            }

            // 完了判定
            if (currentBreakingProgress >= 1.0f) {
                LinearBreakFeature.finishBreaking(minecraft, targetPos, currentBreakingSide ?: side)
                synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
                resetCurrentState()
                miningDelayTimer = 5
            }
        }
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
            LinearBreakFeature.renderSolidBox(graphics3D, pos, currentBreakingProgress, color)
        }
    }

    override fun onEnabled() {
        synchronized(blocksToMine) { blocksToMine.clear() }
        resetCurrentState()
    }
    override fun onDisabled() {
        if (currentBreakingPos != null) gameMode?.stopDestroyBlock()
        synchronized(blocksToMine) { blocksToMine.clear() }
        resetCurrentState()
    }
}
