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

    sealed class Armor : InventoryIndex() {
        object Head : Armor() {
            override fun toContainerSlot(): Int = 5
        }
        object Chest : Armor() {
            override fun toContainerSlot(): Int = 6
        }
        object Legs : Armor() {
            override fun toContainerSlot(): Int = 7
        }
        object Foots : Armor() {
            override fun toContainerSlot(): Int = 8
        }
    }
}
