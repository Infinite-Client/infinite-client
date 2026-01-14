package org.infinite.libs.minecraft.multiplayer.inventory.structs

import org.infinite.libs.interfaces.MinecraftInterface

sealed class InventoryIndex : MinecraftInterface() {
    abstract fun toContainerSlot(): Int

    data object MainHand : InventoryIndex() {
        override fun toContainerSlot(): Int = (player?.inventory?.selectedSlot ?: 0) + 36
    }

    data object OffHand : InventoryIndex() {
        override fun toContainerSlot(): Int = 45
    }

    /** ホットバー (0-8) */
    data class Hotbar(val index: Int) : InventoryIndex() {
        init {
            require(index in 0..8)
        }

        override fun toContainerSlot(): Int = index + 36
    }

    /** メインインベントリ / バックパック (0-26) */
    data class Backpack(val index: Int) : InventoryIndex() {
        init {
            require(index in 0..26)
        }

        override fun toContainerSlot(): Int = index + 9
    }

    data class Armor(val slot: ArmorSlot) : InventoryIndex() {
        enum class ArmorSlot { Head, Chest, Legs, Feet }

        override fun toContainerSlot(): Int = when (slot) {
            ArmorSlot.Head -> 5
            ArmorSlot.Chest -> 6
            ArmorSlot.Legs -> 7
            ArmorSlot.Feet -> 8
        }
    }
}
