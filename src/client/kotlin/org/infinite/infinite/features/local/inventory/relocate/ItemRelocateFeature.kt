package org.infinite.infinite.features.local.inventory.relocate

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.minecraft.multiplayer.inventory.InventorySystem
import org.infinite.libs.minecraft.multiplayer.inventory.structs.InventoryIndex
import org.lwjgl.glfw.GLFW

class ItemRelocateFeature : LocalFeature() {

    override val defaultToggleKey: Int = GLFW.GLFW_KEY_UNKNOWN
    val delayProperty by property(IntProperty(2, 1, 20, "ticks"))

    // RestockFeatureから参照するために公開
    val targetSlots = mutableSetOf<Int>()
    private var tickDelay = 0
    private var wasScreenOpen = false

    override fun onStartTick() {
        val inv = InventorySystem
        if (minecraft.screen != null) {
            wasScreenOpen = true
            return
        }

        if (wasScreenOpen) {
            updateTargetSlots(inv)
            wasScreenOpen = false
            return
        }

        if (player == null || tickDelay > 0) {
            if (tickDelay > 0) tickDelay--
            return
        }

        // カーソルにアイテムがある時は事故防止のため何もしない
        if (!inv.cursorItem().isEmpty) return

        for (i in 0..8) {
            if (!targetSlots.contains(i)) continue

            val hotbarIdx = InventoryIndex.Hotbar(i)
            val stack = inv.getItem(hotbarIdx)

            if (!stack.isEmpty) {
                if (processRelocation(inv, hotbarIdx)) {
                    tickDelay = delayProperty.value
                    // 1回移動させたらこのティックは終了
                    return
                }
            }
        }
    }

    private fun processRelocation(inv: InventorySystem, fromIdx: InventoryIndex): Boolean {
        val stackToMove = inv.getItem(fromIdx)
        if (stackToMove.isEmpty) return false

        // 1. スタック可能なら既存の枠へ
        if (stackToMove.isStackable) {
            for (i in 0..26) {
                val backpackIdx = InventoryIndex.Backpack(i)
                val target = inv.getItem(backpackIdx)
                if (!target.isEmpty && target.item == stackToMove.item && target.count < target.maxStackSize) {
                    inv.swapItems(fromIdx, backpackIdx)
                    return true
                }
            }
        }

        // 2. 空きスロットへ
        for (i in 0..26) {
            val backpackIdx = InventoryIndex.Backpack(i)
            if (inv.getItem(backpackIdx).isEmpty) {
                inv.swapItems(fromIdx, backpackIdx)
                return true
            }
        }
        return false
    }

    fun updateTargetSlots(inv: InventorySystem) {
        targetSlots.clear()
        for (i in 0..8) {
            if (inv.getItem(InventoryIndex.Hotbar(i)).isEmpty) {
                targetSlots.add(i)
            }
        }
    }

    override fun onEnabled() {
        updateTargetSlots(InventorySystem)
        wasScreenOpen = false
    }
}
