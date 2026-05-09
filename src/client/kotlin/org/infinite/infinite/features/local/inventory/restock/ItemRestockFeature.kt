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

    // インデックス 0-8: Hotbar, インデックス 9: Offhand
    private val lastKnownItems = arrayOfNulls<Item>(10)
    private var tickDelay = 0
    private var wasScreenOpen = false

    override fun onStartTick() {
        if (minecraft.screen != null) {
            wasScreenOpen = true
            return
        }

        // 画面を閉じた直後に現在の状態を正しく同期
        if (wasScreenOpen) {
            updateLastKnownItems()
            wasScreenOpen = false
        }

        if (player == null) return

        // ディレイ処理
        if (tickDelay > 0) {
            tickDelay--
            return
        }

        val inv = InventorySystem
        val selectedSlot = player?.inventory?.selectedSlot ?: 0

        // 1. メインハンドのチェック
        val mainHandIdx = InventoryIndex.Hotbar(selectedSlot)
        val restockedMain = checkAndRestock(inv, mainHandIdx, selectedSlot)

        // 2. オフハンドのチェック (メインハンドで操作が発生しなかった場合のみ実行)
        if (!restockedMain) {
            checkAndRestock(inv, InventoryIndex.OffHand, 9)
        }
    }

    private fun checkAndRestock(inv: InventorySystem, targetIdx: InventoryIndex, cacheIdx: Int): Boolean {
        val relocate = InfiniteClient.localFeatures.inventory.itemRelocateFeature

        // Relocate有効時のHotbarスロット保護
        if (targetIdx is InventoryIndex.Hotbar && relocate.isEnabled()) {
            if (relocate.targetSlots.contains(targetIdx.index)) return false
        }

        val currentStack = inv.getItem(targetIdx)
        val currentItem = currentStack.item
        val cachedItem = lastKnownItems[cacheIdx]

        // 補充が必要な条件判定
        val isExpired = currentStack.isEmpty || currentItem == Items.AIR
        val isLow = !isExpired && currentStack.isStackable && currentStack.count <= thresholdProperty.value

        if (isExpired || isLow) {
            // 使い切った場合はキャッシュを、残量がある場合は現在のアイテムを使用
            val itemToFind = if (isExpired) cachedItem else currentItem

            if (itemToFind != null && itemToFind != Items.AIR) {
                // 検索
                val sourceIdx = findItemInInventory(inv, itemToFind, targetIdx)

                if (sourceIdx != null) {
                    inv.swapItems(sourceIdx, targetIdx)
                    tickDelay = delayProperty.value
                    // 移動直後はキャッシュを更新せず、次のTickで新しいアイテムを検知させる
                    return true
                }
            }
        }

        // キャッシュ更新: アイテムを手に持っている時だけ更新する
        // これにより「使い切ってAIRになった瞬間」にキャッシュが上書きされるのを防ぐ
        if (currentItem != Items.AIR) {
            lastKnownItems[cacheIdx] = currentItem
        }

        return false
    }

    private fun findItemInInventory(inv: InventorySystem, item: Item, excludeIdx: InventoryIndex): InventoryIndex? {
        // 1. ホットバーを優先検索
        for (i in 0..8) {
            val idx = InventoryIndex.Hotbar(i)
            if (isSameSlot(idx, excludeIdx)) continue
            val stack = inv.getItem(idx)
            if (!stack.isEmpty && stack.`is`(item)) return idx
        }
        // 2. バックパックを検索
        for (i in 0..26) {
            val idx = InventoryIndex.Backpack(i)
            if (isSameSlot(idx, excludeIdx)) continue
            val stack = inv.getItem(idx)
            if (!stack.isEmpty && stack.`is`(item)) return idx
        }
        return null
    }

    // data objectとdata classの混在でも確実にスロット番号で比較
    private fun isSameSlot(idx1: InventoryIndex, idx2: InventoryIndex): Boolean = idx1.toContainerSlot() == idx2.toContainerSlot()

    fun updateLastKnownItems() {
        val inv = InventorySystem
        for (i in 0..8) {
            lastKnownItems[i] = inv.getItem(InventoryIndex.Hotbar(i)).item
        }
        lastKnownItems[9] = inv.getItem(InventoryIndex.OffHand).item
    }

    override fun onEnabled() {
        updateLastKnownItems()
        wasScreenOpen = false
        tickDelay = 0
    }
}
