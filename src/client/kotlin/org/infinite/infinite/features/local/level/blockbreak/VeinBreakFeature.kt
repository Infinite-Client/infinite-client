package org.infinite.infinite.features.local.level.blockbreak

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.GameMasterBlock
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

    val blockList by property(
        BlockListProperty(
            listOf(
                "minecraft:ancient_debris", "minecraft:coal_ore", "minecraft:copper_ore",
                "minecraft:deepslate_coal_ore", "minecraft:deepslate_copper_ore",
                "minecraft:deepslate_diamond_ore", "minecraft:deepslate_emerald_ore",
                "minecraft:deepslate_gold_ore", "minecraft:deepslate_iron_ore",
                "minecraft:deepslate_lapis_ore", "minecraft:deepslate_redstone_ore",
                "minecraft:diamond_ore", "minecraft:emerald_ore", "minecraft:gold_ore",
                "minecraft:iron_ore", "minecraft:lapis_ore", "minecraft:nether_gold_ore",
                "minecraft:nether_quartz_ore", "minecraft:redstone_ore",
            ),
        ),
    )
    val breakRange by property(DoubleProperty(5.0, 1.0, 6.0, " blocks"))
    val maxBlocks by property(IntProperty(64, 1, 500))
    val remainingCount: Int get() = blocksToMine.size
    val blocksToMine = LinkedHashSet<BlockPos>()
    fun isWorking(): Boolean = isEnabled() && synchronized(blocksToMine) { blocksToMine.isNotEmpty() }
    var currentBreakingPos: BlockPos? = null
    private var currentBreakingSide: Direction? = null
    var currentBreakingProgress: Float = 0.0f
    private var destroyTicks: Float = 0.0f

    override fun onStartTick() {
        val player = player ?: return
        val world = level ?: return
        val mc = minecraft

        // 1. ターゲットの収集 (LinearBreakと同一)
        if (options.keyAttack.isDown) {
            val hit = mc.hitResult
            if (hit != null && hit.type == HitResult.Type.BLOCK) {
                tryAdd((hit as BlockHitResult).blockPos)
            }
        }

        // 2. 範囲外や空気などの無効ブロックを削除 (LinearBreakと同一)
        val rangeSq = breakRange.value * breakRange.value
        synchronized(blocksToMine) {
            blocksToMine.removeAll { pos ->
                pos.distSqr(player.blockPosition()) > rangeSq || world.getBlockState(pos).isAir
            }
        }

        // 3. 採掘ロジックの実行
        mine()
    }

    private fun mine() {
        val world = level ?: return
        val player = player ?: return
        val mc = minecraft

        // 排他制御: LinearBreakが動いている時は何もしない
        val lb = InfiniteClient.localFeatures.level.linearBreakFeature
        if (lb.isEnabled() && lb.currentBreakingPos != null) return

        val targetPos = synchronized(blocksToMine) { blocksToMine.firstOrNull() } ?: run {
            if (currentBreakingPos != null) abortCurrentBreaking(mc)
            return
        }

        val state = world.getBlockState(targetPos)

        // ターゲット変更時の初期化処理
        if (currentBreakingPos != targetPos) {
            if (currentBreakingPos != null) abortCurrentBreaking(mc)

            currentBreakingPos = targetPos
            currentBreakingSide = LinearBreakFeature.getSide(mc, targetPos)
            currentBreakingProgress = 0.0f
            destroyTicks = 0.0f

            if (player.abilities.instabuild) {
                LinearBreakFeature.sendBreakPacket(mc, Action.START_DESTROY_BLOCK, targetPos, currentBreakingSide!!)
                mc.execute {
                    simulateBlockBreak(targetPos)
                    synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
                }
                resetCurrentState()
            } else {
                LinearBreakFeature.sendBreakPacket(mc, Action.START_DESTROY_BLOCK, targetPos, currentBreakingSide!!)
                mc.execute {
                    if (!world.getBlockState(targetPos).isAir && currentBreakingProgress == 0.0f) {
                        state.attack(world, targetPos, player)
                    }
                }
            }
            return
        }

        // 採掘進行
        if (state.isAir) {
            synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
            resetCurrentState()
            return
        }

        // 進捗の加算 (LinearBreakと全く同じ計算)
        currentBreakingProgress += state.getDestroyProgress(player, world, targetPos)

        mc.execute {
            if (destroyTicks % 4.0f == 0.0f) {
                val soundType = state.soundType
                mc.soundManager.play(
                    SimpleSoundInstance(
                        soundType.hitSound,
                        SoundSource.BLOCKS,
                        (soundType.volume + 1.0f) / 8.0f,
                        soundType.pitch * 0.5f,
                        SoundInstance.createUnseededRandom(),
                        targetPos,
                    ),
                )
            }
            world.destroyBlockProgress(player.id, targetPos, (currentBreakingProgress * 10).toInt())
        }

        destroyTicks++
        player.swing(InteractionHand.MAIN_HAND)

        // 破壊完了時の処理
        if (currentBreakingProgress >= 1.0f) {
            val side = currentBreakingSide ?: Direction.UP
            LinearBreakFeature.sendBreakPacket(mc, Action.STOP_DESTROY_BLOCK, targetPos, side)

            mc.execute {
                simulateBlockBreak(targetPos)
                synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
            }
            resetCurrentState()
        }
    }

    private fun simulateBlockBreak(pos: BlockPos) {
        val world = level ?: return
        val player = player ?: return
        val state = world.getBlockState(pos)
        val block = state.block

        if (player.blockActionRestricted(world, pos, if (player.abilities.instabuild) net.minecraft.world.level.GameType.CREATIVE else net.minecraft.world.level.GameType.SURVIVAL)) return
        if (block is GameMasterBlock && !player.canUseGameMasterBlocks()) return

        block.playerWillDestroy(world, pos, state, player)
        val fluid = world.getFluidState(pos)
        val changed = world.setBlock(pos, fluid.createLegacyBlock(), 11)
        if (changed) {
            block.destroy(world, pos, state)
        }
    }

    private fun abortCurrentBreaking(mc: Minecraft) {
        val pos = currentBreakingPos ?: return
        val side = currentBreakingSide ?: Direction.DOWN
        LinearBreakFeature.sendBreakPacket(mc, Action.ABORT_DESTROY_BLOCK, pos, side)
        mc.execute {
            level?.destroyBlockProgress(player?.id ?: 0, pos, -1)
        }
        resetCurrentState()
    }

    private fun resetCurrentState() {
        currentBreakingPos = null
        currentBreakingSide = null
        currentBreakingProgress = 0.0f
        destroyTicks = 0.0f
    }

    fun tryAdd(pos: BlockPos): Boolean {
        val lvl = level ?: return false
        val state = lvl.getBlockState(pos)
        if (state.isAir || state.getDestroySpeed(lvl, pos) < 0) return false
        if (!isOreBlock(state.block)) return false

        synchronized(blocksToMine) {
            if (blocksToMine.contains(pos)) return true
            findVein(pos) // 鉱石であれば周囲を連鎖探索
            return blocksToMine.contains(pos)
        }
    }

    fun isOreBlock(block: Block): Boolean = blockList.value.contains(BuiltInRegistries.BLOCK.getKey(block).toString())

    private fun findVein(startPos: BlockPos) {
        val world = level ?: return
        val queue: Queue<BlockPos> = LinkedList()
        queue.offer(startPos)
        val visited = mutableSetOf<BlockPos>()
        visited.add(startPos)

        val rangeSq = breakRange.value * breakRange.value

        while (queue.isNotEmpty() && blocksToMine.size < maxBlocks.value) {
            val current = queue.poll() ?: break
            if (isOreBlock(world.getBlockState(current).block)) {
                blocksToMine.add(current)

                for (dir in Direction.entries) {
                    val neighbor = current.relative(dir)
                    if (!visited.contains(neighbor) && neighbor.distSqr(startPos) <= rangeSq) {
                        visited.add(neighbor)
                        queue.offer(neighbor)
                    }
                }
            }
        }
    }

    override fun onEnabled() {
        synchronized(blocksToMine) { blocksToMine.clear() }
        resetCurrentState()
    }

    override fun onDisabled() {
        abortCurrentBreaking(minecraft)
        synchronized(blocksToMine) { blocksToMine.clear() }
    }

    override fun onLevelRendering(graphics3D: Graphics3D) {
        val color = InfiniteClient.theme.colorScheme.accentColor
        val list = synchronized(blocksToMine) { blocksToMine.toList() }

        list.forEach { pos ->
            if (pos != currentBreakingPos) {
                graphics3D.boxOptimized(
                    Vec3.atLowerCornerOf(pos),
                    Vec3(pos.x + 1.0, pos.y + 1.0, pos.z + 1.0),
                    color,
                    1.0f,
                    true,
                )
            }
        }

        currentBreakingPos?.let { pos ->
            LinearBreakFeature.renderSolidBox(graphics3D, pos, currentBreakingProgress, color)
        }
    }
}
