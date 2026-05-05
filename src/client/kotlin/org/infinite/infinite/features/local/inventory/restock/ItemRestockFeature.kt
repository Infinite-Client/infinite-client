package org.infinite.infinite.features.local.inventory.restock

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.minecraft.multiplayer.inventory.InventorySystem
import org.infinite.libs.minecraft.multiplayer.inventory.structs.InventoryIndex
import org.lwjgl.glfw.GLFW

class ItemRestockFeature : LocalFeature() {

    override val defaultToggleKey: Int = GLFW.GLFW_KEY_UNKNOWN
    val delayProperty by property(IntProperty(3, 1, 20, "ticks"))
    val thresholdProperty by property(IntProperty(8, 1, 64, "count"))

    // インデックス 0-8: Hotbar, インデックス 9: Offhand 用にサイズを10に拡張
    private val lastKnownItems = arrayOfNulls<Item>(10)
    private var tickDelay = 0
    private var wasScreenOpen = false

    override fun onStartTick() {
        if (minecraft.screen != null) {
            wasScreenOpen = true
            return
        }

        if (wasScreenOpen) {
            updateLastKnownItems()
            wasScreenOpen = false
        }

        if (player == null || tickDelay > 0) {
            if (tickDelay > 0) tickDelay--
            return
        }

        val itemRelocateFeature = InfiniteClient.localFeatures.inventory.itemRelocateFeature
        val inv = InventorySystem

        // 1. メインハンド（選択中のスロット）の補充
        val selectedSlot = player?.inventory?.selectedSlot ?: 0
        checkAndRestock(inv, InventoryIndex.Hotbar(selectedSlot), selectedSlot)

        // 2. オフハンドの補充 (メインハンドで補充が発生しなかった場合のみ、または連続で判定)
        // tickDelayが更新されていなければ実行
        if (tickDelay <= 0) {
            checkAndRestock(inv, InventoryIndex.OffHand, 9) // 9番目をオフハンド用として扱う
        }

        itemRelocateFeature.updateHotbar()
    }

    /**
     * @param targetIdx インベントリ操作用のIndex
     * @param cacheIdx lastKnownItems配列の保存先インデックス
     */
    private fun checkAndRestock(inv: InventorySystem, targetIdx: InventoryIndex, cacheIdx: Int) {
        val relocate = InfiniteClient.localFeatures.inventory.itemRelocateFeature

        // Hotbarのスロットの場合のみRelocateとの競合をチェック
        if (targetIdx is InventoryIndex.Hotbar) {
            if (relocate.isEnabled() && relocate.targetSlots.contains(targetIdx.index)) {
                return
            }
        }

        val currentStack = inv.getItem(targetIdx)
        val lastItem = lastKnownItems[cacheIdx]

        // 補充が必要な条件: 空、またはスタック可能かつ閾値以下
        if (currentStack.isEmpty || (currentStack.count <= thresholdProperty.value && currentStack.isStackable)) {
            val itemToFind = if (currentStack.isEmpty) lastItem else currentStack.item

            if (itemToFind != null && itemToFind != Items.AIR) {
                // 1. Hotbarから探す (現在のスロットは除外)
                // 2. なければバックパックから探す
                val sourceIdx = findItemInHotbar(inv, itemToFind, excludeIdx = targetIdx)
                    ?: findItemInBackpack(inv, itemToFind)

                if (sourceIdx != null) {
                    inv.swapItems(sourceIdx, targetIdx)
                    tickDelay = delayProperty.value
                }
            }
        }

        // キャッシュの更新
        if (!currentStack.isEmpty) {
            lastKnownItems[cacheIdx] = currentStack.item
        }
    }

    private fun findItemInHotbar(inv: InventorySystem, item: Item, excludeIdx: InventoryIndex): InventoryIndex? {
        for (i in 0..8) {
            val idx = InventoryIndex.Hotbar(i)
            if (idx == excludeIdx) continue
            if (inv.getItem(idx).`is`(item)) return idx
        }
        return null
    }

    private fun findItemInBackpack(inv: InventorySystem, item: Item): InventoryIndex? {
        for (i in 0..26) {
            val idx = InventoryIndex.Backpack(i)
            if (inv.getItem(idx).`is`(item)) return idx
        }
        return null
    }

    fun updateLastKnownItems() {
        val inv = InventorySystem
        // Hotbar
        for (i in 0..8) {
            lastKnownItems[i] = inv.getItem(InventoryIndex.Hotbar(i)).item
        }
        // Offhand
        lastKnownItems[9] = inv.getItem(InventoryIndex.OffHand).item
    }

    override fun onEnabled() {
        updateLastKnownItems()
        wasScreenOpen = false
    }
}
