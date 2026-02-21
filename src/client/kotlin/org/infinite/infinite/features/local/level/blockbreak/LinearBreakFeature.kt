package org.infinite.infinite.features.local.level.blockbreak

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.ClipContext
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

    val blocksToMine = LinkedHashSet<BlockPos>()
    var currentBreakingPos: BlockPos? = null
    private var currentBreakingSide: Direction? = null

    // 描画用の進捗管理（gameMode内部の状態を取得できないため保持）
    var currentBreakingProgress: Float = 0.0f

    fun isWorking(): Boolean = isEnabled() && synchronized(blocksToMine) { blocksToMine.isNotEmpty() }

    override fun onStartTick() {
        val player = player ?: return

        // 1. ターゲットの収集 (左クリック押しっぱなし中)
        if (options.keyAttack.isDown) {
            val hit = minecraft.hitResult
            if (hit != null && hit.type == HitResult.Type.BLOCK) {
                tryAdd((hit as BlockHitResult).blockPos)
            }
        }

        // 2. 範囲外のブロックをキューから削除
        val rangeSq = breakRange.value * breakRange.value
        synchronized(blocksToMine) {
            blocksToMine.retainAll { it.distSqr(player.blockPosition()) <= rangeSq }
        }

        // 3. 採掘ロジックの実行
        mine()
    }

    fun tryAdd(pos: BlockPos): Boolean {
        val lvl = level ?: return false
        val state = lvl.getBlockState(pos)

        // 空気、流体、岩盤(破壊不可)を除外
        if (state.isAir || !state.fluidState.isEmpty || state.getDestroySpeed(lvl, pos) < 0) return false

        // VeinBreak(一括破壊)対象はそちらに任せる
        val veinBreak = InfiniteClient.localFeatures.level.veinBreakFeature
        if (veinBreak.isEnabled() && veinBreak.isOreBlock(state.block)) return false

        synchronized(blocksToMine) { return blocksToMine.add(pos) }
    }

    private fun mine() {
        val world = level ?: return
        val player = player ?: return

        val targetPos = synchronized(blocksToMine) { blocksToMine.firstOrNull() } ?: return

        // 距離と空気チェック
        val rangeSq = breakRange.value * breakRange.value
        if (world.getBlockState(targetPos).isAir || targetPos.distSqr(player.blockPosition()) > rangeSq) {
            synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
            resetCurrentState()
            return
        }

        val side = getSide(minecraft, targetPos)

        // クリエイティブの場合
        if (player.abilities.instabuild) {
            instantBreak(minecraft, targetPos, side)
            synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
            return
        }

        // サバイバルの場合
        if (currentBreakingPos != targetPos) {
            currentBreakingPos = targetPos
            currentBreakingProgress = 0.0f
            startBreaking(minecraft, targetPos, side)
        } else {
            // 自前で進捗を計算
            currentBreakingProgress += getProgressPerTick(minecraft, targetPos)

            // 腕振りパケットを継続して送る（掘削中の演出とパケット維持）
            if (minecraft.level!!.gameTime % 2 == 0L) {
                player.swing(InteractionHand.MAIN_HAND)
            }

            if (currentBreakingProgress >= 1.0f) {
                finishBreaking(minecraft, targetPos, side)
                synchronized(blocksToMine) { blocksToMine.remove(targetPos) }
                resetCurrentState()
            }
        }
    }

    private fun resetCurrentState() {
        currentBreakingPos = null
        currentBreakingSide = null
        currentBreakingProgress = 0.0f
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

    override fun onLevelRendering(graphics3D: Graphics3D) {
        val color = InfiniteClient.theme.colorScheme.accentColor
        val list = synchronized(blocksToMine) { blocksToMine.toList() }

        // キュー内のブロックを枠線で表示
        list.forEach { pos ->
            graphics3D.boxOptimized(
                Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()),
                Vec3(pos.x + 1.0, pos.y + 1.0, pos.z + 1.0),
                color,
                1.0f,
                true,
            )
        }

        // 現在破壊中のブロックを中身の詰まったボックスで表示（進捗に合わせて膨らむ）
        currentBreakingPos?.let { pos ->
            renderSolidBox(graphics3D, pos, currentBreakingProgress, color)
        }
    }

    val remainingCount: Int
        get() = blocksToMine.size

    companion object {
        /**
         * ブロック破壊を開始する
         */
        fun startBreaking(minecraft: Minecraft, pos: BlockPos, side: Direction) {
            // 1. サーバーへ破壊開始を通知
            sendBreakPacket(minecraft, Action.START_DESTROY_BLOCK, pos, side)
            // 2. 腕を振る
            minecraft.player?.swing(InteractionHand.MAIN_HAND)
        }

        /**
         * 破壊を完了させる (サバイバル用)
         */
        fun finishBreaking(minecraft: Minecraft, pos: BlockPos, side: Direction) {
            sendBreakPacket(minecraft, Action.STOP_DESTROY_BLOCK, pos, side)
            minecraft.player?.swing(InteractionHand.MAIN_HAND)
        }

        /**
         * クリエイティブモード用の即時破壊
         */
        fun instantBreak(minecraft: Minecraft, pos: BlockPos, side: Direction) {
            // クリエイティブでは START パケットだけで即座に破壊される仕様
            sendBreakPacket(minecraft, Action.START_DESTROY_BLOCK, pos, side)
            minecraft.player?.swing(InteractionHand.MAIN_HAND)
        }

        fun sendBreakPacket(minecraft: Minecraft, action: Action, pos: BlockPos, side: Direction) {
            val packet = ServerboundPlayerActionPacket(action, pos, side)
            minecraft.connection?.send(packet)
        }

        fun getSide(minecraft: Minecraft, pos: BlockPos): Direction {
            val player = minecraft.player ?: return Direction.UP
            val eyePos = player.eyePosition
            val blockCenter = Vec3.atCenterOf(pos)
            val hit = minecraft.level?.clip(ClipContext(eyePos, blockCenter, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))

            if (hit != null && hit.type == HitResult.Type.BLOCK && hit.blockPos == pos) return hit.direction

            val diff = eyePos.subtract(blockCenter)
            return Direction.getApproximateNearest(diff.x.toFloat(), diff.y.toFloat(), diff.z.toFloat())
        }

        fun getProgressPerTick(minecraft: Minecraft, pos: BlockPos): Float {
            val player = minecraft.player ?: return 0f
            val world = minecraft.level ?: return 0f
            val state = world.getBlockState(pos)

            // バニラの破壊進捗計算メソッドを直接使用
            return state.getDestroyProgress(player, world, pos)
        }

        fun renderSolidBox(g: Graphics3D, pos: BlockPos, progress: Float, color: Int) {
            val p = progress.coerceIn(0f, 1f)
            val offset = (1.0 - p) * 0.5
            val min = Vec3(pos.x + offset, pos.y + offset, pos.z + offset).add(0.005, 0.005, 0.005)
            val max = Vec3(pos.x + 1.0 - offset, pos.y + 1.0 - offset, pos.z + 1.0 - offset).subtract(0.005, 0.005, 0.005)

            val (x0, y0, z0) = doubleArrayOf(min.x, min.y, min.z)
            val (x1, y1, z1) = doubleArrayOf(max.x, max.y, max.z)
            val renderColor = (color and 0x00FFFFFF) or 0x60000000
            g.rectangleFill(Vec3(x0, y0, z1), Vec3(x1, y0, z1), Vec3(x1, y0, z0), Vec3(x0, y0, z0), renderColor, false)
            g.rectangleFill(Vec3(x0, y1, z0), Vec3(x1, y1, z0), Vec3(x1, y1, z1), Vec3(x0, y1, z1), renderColor, false)
            g.rectangleFill(Vec3(x1, y0, z0), Vec3(x1, y1, z0), Vec3(x0, y1, z0), Vec3(x0, y0, z0), renderColor, false)
            g.rectangleFill(Vec3(x0, y0, z1), Vec3(x0, y1, z1), Vec3(x1, y1, z1), Vec3(x1, y0, z1), renderColor, false)
            g.rectangleFill(Vec3(x0, y0, z0), Vec3(x0, y1, z0), Vec3(x0, y1, z1), Vec3(x0, y0, z1), renderColor, false)
            g.rectangleFill(Vec3(x1, y0, z1), Vec3(x1, y1, z1), Vec3(x1, y1, z0), Vec3(x1, y0, z0), renderColor, false)
        }
    }
}
