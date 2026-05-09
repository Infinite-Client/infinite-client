package org.infinite.infinite.features.local.level.blockbreak

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.GameMasterBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.level.LocalLevelCategory
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.graphics.Graphics3D

class LinearBreakFeature : LocalFeature() {
    override val featureType = FeatureLevel.Cheat
    override val categoryClass = LocalLevelCategory::class

    val breakRange by property(DoubleProperty(5.0, 1.0, 6.0, " blocks"))
    val remainingCount: Int get() = blocksToMine.size
    val blocksToMine = LinkedHashSet<BlockPos>()
    var currentBreakingPos: BlockPos? = null
    private var currentBreakingSide: Direction? = null
    var currentBreakingProgress: Float = 0.0f
    private var destroyTicks: Float = 0.0f

    fun isWorking(): Boolean = isEnabled() && synchronized(blocksToMine) { blocksToMine.isNotEmpty() }

    override fun onStartTick() {
        val player = player ?: return
        val world = level ?: return

        // 1. ターゲットの収集
        if (options.keyAttack.isDown) {
            val hit = minecraft.hitResult
            if (hit != null && hit.type == HitResult.Type.BLOCK) {
                tryAdd((hit as BlockHitResult).blockPos)
            }
        }

        // 2. 範囲外や空気などの無効ブロックを削除
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

        val targetPos = synchronized(blocksToMine) { blocksToMine.firstOrNull() } ?: run {
            if (currentBreakingPos != null) abortCurrentBreaking(mc)
            return
        }

        val state = world.getBlockState(targetPos)

        // ターゲット変更時の処理
        if (currentBreakingPos != targetPos) {
            if (currentBreakingPos != null) abortCurrentBreaking(mc)

            currentBreakingPos = targetPos
            currentBreakingSide = getSide(mc, targetPos)
            currentBreakingProgress = 0.0f
            destroyTicks = 0.0f

            if (player.abilities.instabuild) {
                sendBreakPacket(mc, Action.START_DESTROY_BLOCK, targetPos, currentBreakingSide!!)
                // メインスレッドで破壊実行
                mc.execute {
                    simulateBlockBreak(targetPos)
                    synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
                }
                resetCurrentState()
            } else {
                sendBreakPacket(mc, Action.START_DESTROY_BLOCK, targetPos, currentBreakingSide!!)
                mc.execute {
                    if (!world.getBlockState(targetPos).isAir && currentBreakingProgress == 0.0f) {
                        state.attack(world, targetPos, player)
                    }
                }
            }
            return
        }

        // サバイバル採掘進行
        if (state.isAir) {
            synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
            resetCurrentState()
            return
        }

        currentBreakingProgress += state.getDestroyProgress(player, world, targetPos)

        // 音・ひび割れ描画をメインスレッドへ委譲
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

        // 破壊完了
        if (currentBreakingProgress >= 1.0f) {
            val side = currentBreakingSide ?: Direction.UP
            sendBreakPacket(mc, Action.STOP_DESTROY_BLOCK, targetPos, side)

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
        sendBreakPacket(mc, Action.ABORT_DESTROY_BLOCK, pos, side)
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

        val veinBreak = InfiniteClient.localFeatures.level.veinBreakFeature
        if (veinBreak.isEnabled() && veinBreak.isOreBlock(state.block)) return false

        synchronized(blocksToMine) { return blocksToMine.add(pos) }
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
            renderSolidBox(graphics3D, pos, currentBreakingProgress, color)
        }
    }

    companion object {
        fun sendBreakPacket(mc: Minecraft, action: Action, pos: BlockPos, side: Direction) {
            mc.connection?.send(ServerboundPlayerActionPacket(action, pos, side))
        }

        fun getSide(mc: Minecraft, pos: BlockPos): Direction {
            val player = mc.player ?: return Direction.UP
            val hit = mc.level?.clip(ClipContext(player.eyePosition, Vec3.atCenterOf(pos), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
            return if (hit?.type == HitResult.Type.BLOCK && hit.blockPos == pos) {
                hit.direction
            } else {
                Direction.getApproximateNearest((player.x - pos.x).toFloat(), (player.y - pos.y).toFloat(), (player.z - pos.z).toFloat())
            }
        }

        fun renderSolidBox(g: Graphics3D, pos: BlockPos, progress: Float, color: Int) {
            val p = progress.coerceIn(0f, 1f)
            val size = p * 0.5
            val x0 = pos.x + 0.5 - size
            val y0 = pos.y + 0.5 - size
            val z0 = pos.z + 0.5 - size
            val x1 = pos.x + 0.5 + size
            val y1 = pos.y + 0.5 + size
            val z1 = pos.z + 0.5 + size
            val renderColor = (color and 0x00FFFFFF) or 0x60000000

            g.rectangleFill(Vec3(x0, y0, z1), Vec3(x1, y0, z1), Vec3(x1, y0, z0), Vec3(x0, y0, z0), renderColor, false)
            g.rectangleFill(Vec3(x0, y1, z0), Vec3(x1, y1, z0), Vec3(x1, y1, z1), Vec3(x0, y1, z1), renderColor, false)
            g.rectangleFill(Vec3(x1, y0, z0), Vec3(x1, y1, z0), Vec3(x0, y1, z0), Vec3(x0, y0, z0), renderColor, false)
            g.rectangleFill(Vec3(x0, y0, z1), Vec3(x0, y1, z1), Vec3(x1, y1, z1), Vec3(x1, y0, z1), renderColor, false)
            g.rectangleFill(Vec3(x0, y0, z0), Vec3(x0, y1, z0), Vec3(x0, y1, z1), Vec3(x0, y0, z1), renderColor, false)
            g.rectangleFill(Vec3(x1, y0, z1), Vec3(x1, y1, z1), Vec3(x1, y1, z0), Vec3(x1, y0, z0), renderColor, false)
        }

        fun getProgressPerTick(minecraft: Minecraft, pos: BlockPos): Float {
            val player = minecraft.player ?: return 0f
            val world = minecraft.level ?: return 0f
            val state = world.getBlockState(pos)

            // バニラの破壊進捗計算メソッドを直接使用
            return state.getDestroyProgress(player, world, pos)
        }
    }
}
