package org.infinite.infinite.features.local.inventory

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.minecraft.multiplayer.inventory.InventorySystem
import org.infinite.libs.minecraft.multiplayer.inventory.structs.InventoryIndex
import org.lwjgl.glfw.GLFW

/**
 * プレイヤーがブロックを掘り始めた際に、そのブロックに対する最適なツールを自動で手に持ちます。
 * 最低限必要なツールレベル以上のグレードのツールを、最高グレードから優先的に選択します。
 */
class SwapToolFeature : LocalFeature() {

    override val defaultToggleKey: Int = GLFW.GLFW_KEY_UNKNOWN

    enum class Method {
        Swap,
        HotBar,
    }

    enum class FineToolStrategy {
        Shears,
        SharpTool,
        Hand,
    }

    val method by property(EnumSelectionProperty(Method.HotBar))
    val fineToolStrategy by property(EnumSelectionProperty(FineToolStrategy.SharpTool))
    val switchDelay by property(IntProperty(5, 0, 20, " ticks"))

    private var previousSelectedSlot: Int = -1
    private var lastMiningTick: Long = 0

    private val materialLevels = mapOf(
        "netherite" to 5,
        "diamond" to 4,
        "iron" to 3,
        "stone" to 2,
        "golden" to 1,
        "wooden" to 0,
    )

    override fun onStartTick() {
        val world = level ?: return
        val currentTime = world.gameTime

        val controller = minecraft.gameMode ?: return
        // Note: In 1.20.4, gameMode.isDestroying is used.
        // We might need to check if there are other features breaking blocks like LinearBreak/VeinBreak.
        // For now, we use the standard gameMode status.
        val isMining = controller.isDestroying
        val blockPos = controller.destroyBlockPos

        if (!isMining) {
            if (currentTime - lastMiningTick >= switchDelay.value) {
                resetTool()
            }
            return
        }

        lastMiningTick = currentTime

        val blockState = world.getBlockState(blockPos)
        val block = blockState.block

        // 1. 特殊ツール判定 (ハサミ等)
        if (isFineToolTarget(block)) {
            handleFineTool()
            return
        }

        // 2. 一般ツール判定
        val bestToolIndex = findBestToolForBlock(blockState)
        if (bestToolIndex != null) {
            val currentlyHeld = InventorySystem.getItem(InventoryIndex.MainHand)
            val bestItem = InventorySystem.getItem(bestToolIndex)

            if (currentlyHeld.item == bestItem.item) return

            handleToolSwitch(bestToolIndex)
        }
    }

    private fun isFineToolTarget(block: Block): Boolean {
        val id = BuiltInRegistries.BLOCK.getKey(block).toString()
        return id.contains("leaves") || id.contains("cobweb") || id.contains("wool")
    }

    private fun handleFineTool() {
        var bestToolIndex: InventoryIndex? = null
        val strategy = fineToolStrategy.value

        when (strategy) {
            FineToolStrategy.Shears -> {
                bestToolIndex = InventorySystem.findFirst(Items.SHEARS)
            }

            FineToolStrategy.SharpTool -> {
                // 剣かクワで一番レベルが高いものを探す
                var highestLevel = -1
                listOf("sword", "hoe").forEach { kind ->
                    materialLevels.forEach { (material, level) ->
                        if (level > highestLevel) {
                            val id = "minecraft:${material}_$kind"
                            val item = BuiltInRegistries.ITEM.get(Identifier.parse(id)).get().value()
                            if (item != Items.AIR) {
                                val found = InventorySystem.findFirst(item)
                                if (found != null) {
                                    highestLevel = level
                                    bestToolIndex = found
                                }
                            }
                        }
                    }
                }
            }

            FineToolStrategy.Hand -> return
        }

        if (bestToolIndex != null) {
            val currentlyHeld = InventorySystem.getItem(InventoryIndex.MainHand)
            val bestItem = InventorySystem.getItem(bestToolIndex)
            if (currentlyHeld.item != bestItem.item) {
                handleToolSwitch(bestToolIndex)
            }
        }
    }

    private fun findBestToolForBlock(state: net.minecraft.world.level.block.state.BlockState): InventoryIndex? {
        val gameMode = minecraft.gameMode ?: return null
        val level = level ?: return null
        var bestIndex: InventoryIndex? = null
        var maxSpeed = state.getDestroySpeed(level, gameMode.destroyBlockPos)

        // 1.20では手に持っているアイテムとの相性をチェックするのが確実
        // ホットバーとバックパックを検索
        for (i in 0..35) {
            val idx = if (i < 9) InventoryIndex.Hotbar(i) else InventoryIndex.Backpack(i - 9)
            val stack = InventorySystem.getItem(idx)
            if (stack.isEmpty) continue

            // そのツールがブロックに適しているか、且つ速度が速いか
            val speed = stack.getDestroySpeed(state)
            if (speed > maxSpeed) {
                // 収穫可能かどうかも考慮（オプション）
                if (state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) {
                    continue
                }
                maxSpeed = speed
                bestIndex = idx
            }
        }
        return bestIndex
    }

    private fun handleToolSwitch(bestToolIndex: InventoryIndex) {
        val player = player ?: return
        val currentSlot = player.inventory.selectedSlot

        if (previousSelectedSlot == -1) {
            previousSelectedSlot = currentSlot
        }

        when (method.value) {
            Method.Swap -> {
                InventorySystem.swapItems(InventoryIndex.Hotbar(currentSlot), bestToolIndex)
            }

            Method.HotBar -> {
                if (bestToolIndex is InventoryIndex.Hotbar) {
                    player.inventory.selectedSlot = bestToolIndex.index
                } else if (bestToolIndex is InventoryIndex.Backpack) {
                    // バックパックにある場合はホットバーへスワップしてから切り替えるか、
                    // または単にスワップする
                    InventorySystem.swapItems(InventoryIndex.Hotbar(currentSlot), bestToolIndex)
                }
            }
        }
    }

    private fun resetTool() {
        val player = player ?: return
        if (previousSelectedSlot == -1) return

        val currentSlot = player.inventory.selectedSlot

        when (method.value) {
            Method.Swap -> {
                InventorySystem.swapItems(InventoryIndex.Hotbar(currentSlot), InventoryIndex.Hotbar(previousSelectedSlot))
            }

            Method.HotBar -> {
                player.inventory.selectedSlot = previousSelectedSlot
            }
        }
        previousSelectedSlot = -1
    }

    override fun onDisabled() {
        resetTool()
    }
}
