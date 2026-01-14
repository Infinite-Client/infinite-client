package org.infinite.libs.minecraft.multiplayer.inventory

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.minecraft.multiplayer.inventory.structs.InventoryIndex

object InventorySystem : MinecraftInterface() {

    private val isCreative: Boolean
        get() = player?.isCreative ?: false

    /**
     * 特定のインデックスにアイテムを強制的にセットします。
     * クリエイティブモード時はパケットを直接送信し、サバイバル時はスワップを試行します。
     */
    fun set(index: InventoryIndex, stack: ItemStack): Boolean {
        val p = player ?: return false
        val slotNum = index.toContainerSlot()
        if (slotNum == -1) return false

        if (isCreative) {
            // クリエイティブ時のパケット: ServerboundSetCreativeModeSlotPacket
            // 内部インベントリにセット
            p.inventory.setItem(indexToInventoryArray(index), stack)
            // サーバーへ通知 (スロット番号とアイテムスタック)
            connection?.send(ServerboundSetCreativeModeSlotPacket(slotNum, stack))
            return true
        } else {
            // サバイバル時は「そのアイテムをインベントリから探してスワップする」という古いコードの挙動を再現
            val sourceIndex = findFirst(stack.item) ?: return false
            swapItems(sourceIndex, index)
            return true
        }
    }

    /**
     * 内部配列(Inventoryクラス)のインデックスに変換するヘルパー
     * ※toContainerSlot(メニュー用)とは番号が異なります
     */
    private fun indexToInventoryArray(index: InventoryIndex): Int {
        return when (index) {
            is InventoryIndex.Hotbar -> index.index
            is InventoryIndex.Backpack -> index.index + 9
            is InventoryIndex.Armor -> when (index.slot) {
                InventoryIndex.Armor.ArmorSlot.Feet -> 0
                InventoryIndex.Armor.ArmorSlot.Legs -> 1
                InventoryIndex.Armor.ArmorSlot.Chest -> 2
                InventoryIndex.Armor.ArmorSlot.Head -> 3
            }

            is InventoryIndex.OffHand -> 0 // offhandは別の配列(offhandList)のため、使用箇所で注意が必要
            is InventoryIndex.MainHand -> player?.inventory?.selectedSlot ?: 0
        }
    }

    // --- 以下、以前の実装と同様 ---

    fun swapItems(from: InventoryIndex, to: InventoryIndex) {
        val controller = minecraft.gameMode ?: return
        val p = player ?: return
        val containerId = p.inventoryMenu.containerId

        val s1 = from.toContainerSlot()
        val s2 = to.toContainerSlot()
        if (s1 == -1 || s2 == -1) return

        controller.handleInventoryMouseClick(containerId, s1, 0, ClickType.PICKUP, p)
        controller.handleInventoryMouseClick(containerId, s2, 0, ClickType.PICKUP, p)
        controller.handleInventoryMouseClick(containerId, s1, 0, ClickType.PICKUP, p)

        // カーソル残留チェック
        if (!p.inventoryMenu.carried.isEmpty) {
            val target = findFirstEmptyBackpack()?.toContainerSlot() ?: -999
            controller.handleInventoryMouseClick(containerId, target, 0, ClickType.PICKUP, p)
        }
    }

    /**
     * インベントリ内の最初の空き枠(Backpack)を検索
     */
    fun findFirstEmptyBackpack(): InventoryIndex.Backpack? {
        for (i in 0..26) {
            val idx = InventoryIndex.Backpack(i)
            if (getItem(idx).isEmpty) return idx
        }
        return null
    }

    fun getItem(index: InventoryIndex): ItemStack {
        val p = player ?: return ItemStack.EMPTY
        val slotNum = index.toContainerSlot()
        val menu = p.inventoryMenu
        if (slotNum < 0 || slotNum >= menu.slots.size) return ItemStack.EMPTY
        return menu.getSlot(slotNum).item
    }

    /**
     * 全インベントリから特定のアイテムを1つ検索
     */
    fun findFirst(item: Item): InventoryIndex? {
        // Hotbar
        for (i in 0..8) if (getItem(InventoryIndex.Hotbar(i)).`is`(item)) return InventoryIndex.Hotbar(i)
        // Backpack
        for (i in 0..26) if (getItem(InventoryIndex.Backpack(i)).`is`(item)) return InventoryIndex.Backpack(i)
        // Offhand
        if (getItem(InventoryIndex.OffHand).`is`(item)) return InventoryIndex.OffHand
        return null
    }

    /**
     * 空きスロットの総数
     */
    val emptySlotsCount: Int
        get() = (0..8).count { getItem(InventoryIndex.Hotbar(it)).isEmpty } +
            (0..26).count { getItem(InventoryIndex.Backpack(it)).isEmpty }
}
