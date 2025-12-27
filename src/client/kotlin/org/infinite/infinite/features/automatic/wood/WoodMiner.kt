package org.infinite.infinite.features.automatic.wood

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.infinite.libs.ai.AiInterface
import org.infinite.libs.ai.actions.block.MineBlockAction
import org.infinite.libs.ai.actions.movement.BlockPosMovementAction
import org.infinite.libs.ai.actions.movement.LinearMovementAction
import org.infinite.libs.ai.interfaces.AiAction.AiActionState
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting

class WoodMiner : ConfigurableFeature() {
    val searchRadius =
        FeatureSetting.IntSetting(
            name = "SearchRadius",
            defaultValue = 16,
            min = 8,
            max = 64,
        )
    val searchHeight = FeatureSetting.IntSetting("SearchHeight", 8, 4, 32)
    val woodTypes =
        FeatureSetting.BlockListSetting(
            name = "WoodTypes",
            defaultValue = mutableListOf(),
        )
    override val settings: List<FeatureSetting<*>> =
        listOf(
            searchRadius,
            searchHeight,
            woodTypes,
        )

    override fun onStart() = disable()

    private fun isLogBlock(blockState: BlockState): Boolean = isLogBlock(blockState.block)

    private fun isLogBlock(block: Block): Boolean {
        val logList =
            listOf(
                Blocks.OAK_LOG,
                Blocks.BIRCH_LOG,
                Blocks.ACACIA_LOG,
                Blocks.CHERRY_LOG,
                Blocks.JUNGLE_LOG,
                Blocks.PALE_OAK_LOG,
                Blocks.DARK_OAK_LOG,
                Blocks.MANGROVE_LOG,
                Blocks.SPRUCE_LOG,
            )
        // MANGROVE_LOGが重複していたため一つ削除
        return logList.contains(block) || isCustomLogBlock(block)
    }

    private fun isCustomLogBlock(block: Block): Boolean {
        val blockId = BuiltInRegistries.BLOCK.getKey(block).toString()
        return woodTypes.value.contains(blockId)
    }

    data class Tree(
        val rootPos: BlockPos,
        val count: Int,
        val type: Block,
        val logBlocks: Set<BlockPos>,
    )

    open class State {
        class Idle : State()

        class Goto(
            val pos: BlockPos,
            val isRandomMode: Boolean = false,
        ) : State() {
            var registered = false
        }

        class Mine(
            val tree: Tree,
        ) : State() {
            var registered = false
        }

        // 🌟 アイテム回収のための新しいステートを追加
        class CollectItem(
            val logBlocks: Set<BlockPos>,
        ) : State() {
            var registered = false
        }
    }

    var state: State = State.Idle()

    // searchTreesのコードは変更なし
    private fun searchTrees(): List<Tree> {
        // ... (省略: searchTreesの元のコード)
        val playerPos = player?.blockPosition() ?: return emptyList()
        val r = searchRadius.value
        val h = searchHeight.value
        val trees = mutableListOf<Tree>()
        val searchedPositions = mutableSetOf<BlockPos>() // 既に探索したブロックを記憶

        // 探索範囲: XとZはプレイヤーを中心に ±r、Yはプレイヤーを中心に -1 (下) から +h (上)
        for (x in (playerPos.x - r)..(playerPos.x + r)) {
            for (z in (playerPos.z - r)..(playerPos.z + r)) {
                for (y in (playerPos.y - 1)..(playerPos.y + h)) {
                    val currentPos = BlockPos(x, y, z)

                    // 既に探索済みの位置はスキップ
                    if (searchedPositions.contains(currentPos)) continue

                    val blockState = world!!.getBlockState(currentPos)
                    if (isLogBlock(blockState)) {
                        // 丸太ブロックを見つけたら、その木の全体を探索
                        val logType = blockState.block
                        val woodDetails = searchTreeFromRoot(currentPos, logType)

                        // 見つかった丸太の根元の位置と総数を記録
                        if (woodDetails.count > 0) {
                            trees.add(woodDetails)
                            // 探索済みの丸太ブロックの位置をSetに追加
                            searchedPositions.addAll(woodDetails.logBlocks)
                        }
                    }
                }
            }
        }
        return trees.sortedBy { it.rootPos.distSqr(playerPos) }
    }

    // searchTreeFromRootのコードは変更なし
    private fun searchTreeFromRoot(
        startPos: BlockPos,
        logType: Block,
    ): Tree {
        // ... (省略: searchTreeFromRootの元のコード)
        var rootPos = startPos
        var count = 0
        val queue = ArrayDeque<BlockPos>()
        val visitedLogBlocks = mutableSetOf<BlockPos>() // 探索済みの丸太ブロックの位置を保持
        queue.add(startPos)
        visitedLogBlocks.add(startPos)

        // BFS (幅優先探索) で繋がっている丸太をすべてカウント
        while (queue.isNotEmpty()) {
            val currentPos = queue.removeFirst()
            count++

            // 最も低い丸太の位置を更新
            if (currentPos.y < rootPos.y) {
                rootPos = currentPos
            }

            // 上下左右前後の6方向をチェック
            for (offset in listOf(
                Vec3i(0, 1, 0),
                Vec3i(0, -1, 0),
                Vec3i(1, 0, 0),
                Vec3i(-1, 0, 0),
                Vec3i(0, 0, 1),
                Vec3i(0, 0, -1),
            )) {
                val nextPos = currentPos.offset(offset)
                if (!visitedLogBlocks.contains(nextPos)) {
                    val block = world!!.getBlockState(nextPos).block
                    // 同じ種類の丸太ブロックであるかを確認
                    if (block == logType) {
                        visitedLogBlocks.add(nextPos)
                        queue.add(nextPos)
                    }
                }
            }
        }
        return Tree(rootPos, count, logType, visitedLogBlocks)
    }

    var trees: List<Tree> = emptyList()
    var currentTree: Tree? = null

    override fun onEnabled() {
        state = State.Idle()
        trees = emptyList()
        currentTree = null
    }

    override fun onTick() {
        when (state) {
            is State.Idle -> {
                handleIdle()
            }

            is State.Goto -> {
                handleGoto((state as State.Goto))
            }

            is State.Mine -> {
                handleMine((state as State.Mine))
            }

            // 🌟 新しいステートのハンドリングを追加
            is State.CollectItem -> {
                handleCollectItem((state as State.CollectItem))
            }
        }
    }

    private fun handleIdle() {
        trees = searchTrees()
        if (trees.isEmpty()) {
            randomWalk()
        } else {
            currentTree = trees.first()
            state = State.Goto(currentTree!!.rootPos)
        }
    }

    private fun randomWalk() {
        val r = searchRadius.value
        state = State.Goto(player!!.blockPosition().offset((-r..r).random(), 0, (-r..r).random()), isRandomMode = true)
    }

    private fun handleGoto(goto: State.Goto) {
        if (!goto.registered) {
            goto.registered = true
            val pos = goto.pos
            AiInterface.add(
                BlockPosMovementAction(
                    pos.x,
                    if (goto.isRandomMode) null else pos.y,
                    pos.z,
                    1,
                    0,
                    stateRegister = { if (isEnabled()) null else AiActionState.Failure },
                    onSuccessAction = { state = State.Mine(currentTree!!) },
                    onFailureAction = { disable() },
                ),
            )
        }
    }

    // 🌟 Mineの成功後にCollectItemステートに遷移するように修正
    private fun handleMine(mine: State.Mine) {
        if (!mine.registered) {
            mine.registered = true
            val logBlocks = mine.tree.logBlocks
            AiInterface.add(
                MineBlockAction(
                    logBlocks.toMutableList(),
                    stateRegister = { if (isEnabled()) null else AiActionState.Failure },
                    // 🌟 採掘成功後、アイテム回収ステートに遷移し、丸太の座標リストを渡す
                    onSuccessAction = { state = State.CollectItem(logBlocks) },
                    onFailureAction = { disable() },
                ),
            )
        }
    }

    // 🌟 アイテム回収のための新しいハンドラを実装
    private fun handleCollectItem(collect: State.CollectItem) {
        if (!collect.registered) {
            collect.registered = true

            // 採掘した丸太のブロック座標セットから、最も低い位置にあるブロック、
            // もしくは何らかの中心的な位置の座標を取得します。
            // ここでは、丸太ブロックの座標を全て含むバウンディングボックスの中心を回収目標とします。
            val minX = collect.logBlocks.minOf { it.x }
            val minY = collect.logBlocks.minOf { it.y }
            val minZ = collect.logBlocks.minOf { it.z }
            val maxX = collect.logBlocks.maxOf { it.x }
            val maxZ = collect.logBlocks.maxOf { it.z }

            // バウンディングボックスの中心のVec3dを計算
            // アイテムはブロックの底面付近にドロップするため、Y座標は最も低いブロックのY座標 + 0.5とします
            val targetX = (minX + maxX) / 2.0 + 0.5
            val targetY = minY + 0.5 // アイテムはブロックの中心ではなく、地面付近にドロップする
            val targetZ = (minZ + maxZ) / 2.0 + 0.5

            val targetPos = Vec3(targetX, targetY, targetZ)

            // LinearMovementActionでアイテムドロップ位置に移動
            AiInterface.add(
                LinearMovementAction(
                    pos = targetPos,
                    movementRange = 1.0, // 3ブロック以内まで近づけば回収できるはず
                    heightRange = null, // Y軸の制限は特に設けない（垂直方向の移動は必要ない場合が多い）
                    onSuccessAction = {
                        // 回収が完了したら、次の木を探すためにIdleステートに戻る
                        state = State.Idle()
                    },
                    onFailureAction = {
                        state = State.Idle()
                    },
                ),
            )
        }
    }
}
