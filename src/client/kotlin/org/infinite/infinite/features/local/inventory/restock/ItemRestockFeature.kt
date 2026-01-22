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

    private val lastKnownItems = arrayOfNulls<Item>(9)
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

        val inv = InventorySystem
        val selectedSlot = player?.inventory?.selectedSlot ?: 0

        // 1. メインハンドを優先
        if (checkAndRestock(inv, selectedSlot, isMainHand = true)) return

        // 2. 他のスロット
        for (i in 0..8) {
            if (i == selectedSlot) continue
            if (checkAndRestock(inv, i, isMainHand = false)) return
        }
    }

    private fun checkAndRestock(inv: InventorySystem, slotIndex: Int, isMainHand: Boolean): Boolean {
        val relocate = InfiniteClient.localFeatures.inventory.itemRelocateFeature
        if (relocate.isEnabled() && relocate.targetSlots.contains(slotIndex)) {
            return false
        }

        val hotbarIdx = InventoryIndex.Hotbar(slotIndex)
        val currentStack = inv.getItem(hotbarIdx)
        val lastItem = lastKnownItems[slotIndex]

        if (currentStack.isEmpty || (currentStack.count <= thresholdProperty.value && currentStack.isStackable)) {
            val itemToFind = if (currentStack.isEmpty) lastItem else currentStack.item

            if (itemToFind != null && itemToFind != Items.AIR) {
                // ホットバー内検索（メインハンド時）
                if (isMainHand) {
                    val otherHotbarIdx = findItemInHotbar(inv, itemToFind, excludeSlot = slotIndex)
                    if (otherHotbarIdx != null) {
                        inv.swapItems(otherHotbarIdx, hotbarIdx)
                        tickDelay = delayProperty.value
                        return true
                    }
                }

                // バックパック内検索
                val backpackIdx = findItemInBackpack(inv, itemToFind)
                if (backpackIdx != null) {
                    inv.swapItems(backpackIdx, hotbarIdx)
                    tickDelay = delayProperty.value
                    return true
                }
            }
        }

        if (!currentStack.isEmpty) {
            lastKnownItems[slotIndex] = currentStack.item
        }
        return false
    }
    private fun findItemInHotbar(inv: InventorySystem, item: Item, excludeSlot: Int): InventoryIndex? {
        for (i in 0..8) {
            if (i == excludeSlot) continue
            val idx = InventoryIndex.Hotbar(i)
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

    private fun updateLastKnownItems() {
        val inv = InventorySystem
        for (i in 0..8) {
            lastKnownItems[i] = inv.getItem(InventoryIndex.Hotbar(i)).item
        }
    }

    override fun onEnabled() {
        updateLastKnownItems()
        wasScreenOpen = false
    }
}
