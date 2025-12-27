package org.infinite.libs.client.inventory

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.infinite.libs.client.player.ClientInterface

object ContainerManager : ClientInterface() {
    sealed class ContainerType {
        object None : ContainerType()

        object Inventory : ContainerType()

        data class Generic(
            val size: Int,
        ) : ContainerType()

        object Generic3x3 : ContainerType()

        object Crafting : ContainerType()

        object Crafter3x3 : ContainerType()

        object Furnace : ContainerType()

        object Smoker : ContainerType()

        object BlastFurnace : ContainerType()

        object Anvil : ContainerType()

        object Beacon : ContainerType()

        object BrewingStand : ContainerType()

        object Enchantment : ContainerType()

        object Grindstone : ContainerType()

        object Hopper : ContainerType()

        object Lectern : ContainerType()

        object Loom : ContainerType()

        object Merchant : ContainerType()

        object ShulkerBox : ContainerType()

        object Smithing : ContainerType()

        object Cartography : ContainerType()

        object Stonecutter : ContainerType()
    }

    sealed class ContainerIndex {
        // 汎用 (チェスト, バレル, シュルカーなど)
        data class Generic(
            val index: Int,
        ) : ContainerIndex() {
            init {
                require(index >= 0) { "Index must be non-negative" }
            }
        }

        // 溶鉱炉系 (Furnace, Smoker, BlastFurnace)
        sealed class FurnaceLike : ContainerIndex() {
            object Material : FurnaceLike() // 0: 材料

            object Fuel : FurnaceLike() // 1: 燃料

            object Product : FurnaceLike() // 2: 結果
        }

        // 醸造台 (BrewingStand)
        sealed class Brewing : ContainerIndex() {
            object Ingredient : Brewing() // 3: 醸造材料

            object Fuel : Brewing() // 4: 燃料 (ブレイズパウダー)

            data class Bottle(
                val index: Int,
            ) : Brewing() { // 0, 1, 2: ポーション
                init {
                    require(index in 0..2) { "Bottle index must be 0, 1, or 2" }
                }
            }
        }

        // 鍛冶台 (Smithing) - ネザライト装備など
        sealed class Smithing : ContainerIndex() {
            object Base : Smithing() // 0: ベースアイテム (例: ダイヤモンドの剣)

            object Addition : Smithing() // 1: 追加材料 (例: ネザライトインゴット)

            object Product : Smithing() // 2: 結果
        }

        // 金床 (Anvil)
        sealed class Anvil : ContainerIndex() {
            object Left : Anvil() // 0: 左スロット

            object Right : Anvil() // 1: 右スロット

            object Product : Anvil() // 2: 結果
        }

        // ホッパー/ディスペンサー/ドロッパー (Generic3x3 / Hopper)
        data class SingleSlot(
            val index: Int,
        ) : ContainerIndex() {
            init {
                require(index in 0..8) { "SingleSlot index must be between 0 and 8" }
            }
        }

        // プレイヤーインベントリ (メイン 27 スロット: 上から 3x9)
        data class PlayerInventory(
            val index: Int,
        ) : ContainerIndex() {
            init {
                // インベントリの左上から右下へ 0..26
                require(index in 0..26) { "PlayerInventory index must be between 0 and 26" }
            }
        }

        // ホットバー (9 スロット: 左から右へ)
        data class Hotbar(
            val index: Int,
        ) : ContainerIndex() {
            init {
                // ホットバーの左から右へ 0..8
                require(index in 0..8) { "Hotbar index must be between 0 and 8" }
            }
        }
    }

    // (open, close, swap, cursorItem, containerType の実装は省略 - 変更なし)

    fun open(pos: BlockPos): Boolean {
        val currentPlayer = player ?: return false
        val interaction = interactionManager ?: return false

        interaction.useItemOn(
            currentPlayer,
            currentPlayer.usedItemHand,
            BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false),
        )
        return true
    }

    fun close() {
        if (client.screen != null) {
            player?.clientSideCloseContainer()
        }
    }

    fun get(index: ContainerIndex): ItemStack? {
        val screenHandler = player?.containerMenu ?: return null
        val currentScreenId = screenHandler.containerId

        // プレイヤーの標準インベントリ画面 (ID 0) の場合はコンテナ操作ではないため null (これは不要なチェックかもしれませんが、元コードに合わせて残します)
        // if (currentScreenId == 0) return null

        // indexToNetworkSlot 内でコンテナタイプのチェックが行われる
        val containerSlotId = indexToNetworkSlot(index) ?: return null

        // 画面ハンドラーからアイテムスタックを取得
        return screenHandler.slots.getOrNull(containerSlotId)?.item
    }

    fun swap(
        from: ContainerIndex,
        to: ContainerIndex,
    ): Boolean {
        val currentPlayer = player ?: return false
        val interaction = interactionManager ?: return false
        val currentScreenId = currentPlayer.containerMenu.containerId

        // indexToNetworkSlot 内でコンテナタイプのチェックが行われる
        val netFrom = indexToNetworkSlot(from) ?: return false // ★不正なIndexは中止★
        val netTo = indexToNetworkSlot(to) ?: return false // ★不正なIndexは中止★

        try {
            // 3クリック スワップシーケンス
            interaction.handleInventoryMouseClick(currentScreenId, netFrom, 0, ClickType.PICKUP, currentPlayer)
            interaction.handleInventoryMouseClick(currentScreenId, netTo, 0, ClickType.PICKUP, currentPlayer)
            interaction.handleInventoryMouseClick(currentScreenId, netFrom, 0, ClickType.PICKUP, currentPlayer)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun cursorItem(): ItemStack? = player?.containerMenu?.carried

    /**
     * 現在開いている画面ハンドラーに基づいて、コンテナのタイプを識別して返します。
     */
    fun containerType(): ContainerType {
        val handler = player?.containerMenu ?: return ContainerType.None
        val handlerType = handler.type

        return when (handlerType) {
            // プレイヤーインベントリ/汎用チェスト系
            MenuType.GENERIC_9x1 -> {
                ContainerType.Inventory
            }

            MenuType.GENERIC_9x2,
            MenuType.GENERIC_9x3,
            MenuType.GENERIC_9x4,
            MenuType.GENERIC_9x5,
            MenuType.GENERIC_9x6,
            -> {
                // handler.slots.size からプレイヤーインベントリの標準スロット数 36 を引いてコンテナサイズを推定
                val containerSize = handler.slots.size - 36
                ContainerType.Generic(containerSize)
            }

            // 3x3 グリッド系
            MenuType.GENERIC_3x3 -> {
                ContainerType.Generic3x3
            }

            MenuType.CRAFTER_3x3 -> {
                ContainerType.Crafter3x3
            }

            // 溶鉱炉系
            MenuType.FURNACE -> {
                ContainerType.Furnace
            }

            MenuType.SMOKER -> {
                ContainerType.Smoker
            }

            MenuType.BLAST_FURNACE -> {
                ContainerType.BlastFurnace
            }

            // ツール/特殊系
            MenuType.ANVIL -> {
                ContainerType.Anvil
            }

            MenuType.BEACON -> {
                ContainerType.Beacon
            }

            MenuType.BREWING_STAND -> {
                ContainerType.BrewingStand
            }

            MenuType.CRAFTING -> {
                ContainerType.Crafting
            }

            MenuType.ENCHANTMENT -> {
                ContainerType.Enchantment
            }

            MenuType.GRINDSTONE -> {
                ContainerType.Grindstone
            }

            MenuType.HOPPER -> {
                ContainerType.Hopper
            }

            MenuType.LECTERN -> {
                ContainerType.Lectern
            }

            MenuType.LOOM -> {
                ContainerType.Loom
            }

            MenuType.MERCHANT -> {
                ContainerType.Merchant
            }

            MenuType.SHULKER_BOX -> {
                ContainerType.ShulkerBox
            }

            MenuType.SMITHING -> {
                ContainerType.Smithing
            }

            MenuType.CARTOGRAPHY_TABLE -> {
                ContainerType.Cartography
            }

            MenuType.STONECUTTER -> {
                ContainerType.Stonecutter
            }

            else -> {
                ContainerType.None
            }
        }
    }

    /**
     * 現在開いている画面ハンドラーのコンテナスロット数を返します。
     * プレイヤーインベントリ (Inventory Type) の場合は 0 を返します。
     */
    private fun getContainerSlotCount(): Int {
        val handler = player?.containerMenu ?: return 0
        return when (val type = containerType()) {
            is ContainerType.Generic -> {
                type.size
            }

            ContainerType.ShulkerBox -> {
                27
            }

            // ShulkerBox は Generic に含まれないため別途定義
            ContainerType.Furnace, ContainerType.Smoker, ContainerType.BlastFurnace -> {
                3
            }

            ContainerType.BrewingStand -> {
                5
            }

            ContainerType.Smithing, ContainerType.Anvil -> {
                3
            }

            ContainerType.Hopper, ContainerType.Generic3x3 -> {
                9
            }

            ContainerType.Inventory -> {
                0
            }

            // プレイヤーインベントリ画面の場合、コンテナスロットは 0 として扱う
            // その他のタイプも、必要に応じて手動でスロット数を定義するか、handler.slots.size - 36 (プレイヤーインベントリ数) を使用します。
            // 既存の ContainerIndex でサポートされているタイプのみスロット数を定義します。
            else -> {
                // その他のコンテナは、Slotsの合計数からプレイヤーインベントリの36スロットを引いて推定します。
                // ただし、クラフティング/エンチャント/金床など、追加スロットを持つものもありますが、ここでは単純な推定を行います。
                val totalSlots = handler.slots.size
                // プレイヤーインベントリのスロットが 36 ではないハンドラーもある (例: Anvil, Beacon, Enchantment)
                // プレイヤーのインベントリ/ホットバーは常にコンテナスロットの後に来るため、プレイヤーインベントリの始点を知る必要があります。
                // プレイヤーインベントリのスロットIDは 0 から始まるため、以下は最も安全な方法です。
                // ここでは、定義済みの ContainerIndex でサポートされているコンテナタイプ以外は、プレイヤーインベントリの計算を安全にするために 0 とします。
                0
            }
        }
    }

    /**
     * ContainerIndexを現在開いている画面ハンドラーのネットワークスロットIDに変換します。
     * 現在開いているコンテナのタイプと Index が一致しない場合、nullを返します。
     */
    private fun indexToNetworkSlot(index: ContainerIndex): Int? {
        val currentType = containerType() // 現在のコンテナタイプを取得
        val containerSlotCount = getContainerSlotCount() // コンテナスロットの総数 (0, 1, 2, ...)

        val containerSlotId =
            when (index) {
                is ContainerIndex.Generic -> {
                    // Generic Index は Generic または ShulkerBox のみで有効
                    if (currentType !is ContainerType.Generic && currentType != ContainerType.ShulkerBox) return null
                    val maxSize = (currentType as? ContainerType.Generic)?.size ?: 27
                    if (index.index >= maxSize) return null
                    index.index
                }

                is ContainerIndex.FurnaceLike -> {
                    if (currentType != ContainerType.Furnace && currentType != ContainerType.Smoker &&
                        currentType != ContainerType.BlastFurnace
                    ) {
                        return null
                    }
                    when (index) {
                        ContainerIndex.FurnaceLike.Material -> 0
                        ContainerIndex.FurnaceLike.Fuel -> 1
                        ContainerIndex.FurnaceLike.Product -> 2
                    }
                }

                // ... (その他のコンテナIndexの処理は変更なし) ...
                is ContainerIndex.Brewing -> {
                    if (currentType != ContainerType.BrewingStand) return null
                    when (index) {
                        is ContainerIndex.Brewing.Bottle -> index.index
                        ContainerIndex.Brewing.Ingredient -> 3
                        ContainerIndex.Brewing.Fuel -> 4
                    }
                }

                is ContainerIndex.Smithing -> {
                    if (currentType != ContainerType.Smithing) return null
                    when (index) {
                        ContainerIndex.Smithing.Base -> 0
                        ContainerIndex.Smithing.Addition -> 1
                        ContainerIndex.Smithing.Product -> 2
                    }
                }

                is ContainerIndex.Anvil -> {
                    if (currentType != ContainerType.Anvil) return null
                    when (index) {
                        ContainerIndex.Anvil.Left -> 0
                        ContainerIndex.Anvil.Right -> 1
                        ContainerIndex.Anvil.Product -> 2
                    }
                }

                is ContainerIndex.SingleSlot -> {
                    if (currentType != ContainerType.Hopper && currentType != ContainerType.Generic3x3) return null
                    index.index
                }

                // 新規: プレイヤーインベントリ (メイン 27 スロット)
                is ContainerIndex.PlayerInventory -> {
                    // PlayerInventory のネットワークスロットIDは: (コンテナスロット数) + (インベントリのオフセット)
                    val offset = index.index // 0..26
                    containerSlotCount + offset
                }

                // 新規: ホットバー (9 スロット)
                is ContainerIndex.Hotbar -> {
                    // Hotbar のネットワークスロットIDは: (コンテナスロット数) + (インベントリのメインスロット 27) + (ホットバーのオフセット)
                    val offset = index.index // 0..8
                    containerSlotCount + 27 + offset
                }
            }

        // プレイヤーインベントリとホットバーのネットワークスロットIDは計算済み
        if (index is ContainerIndex.PlayerInventory || index is ContainerIndex.Hotbar) {
            return containerSlotId
        }

        // コンテナスロットID (0, 1, 2, ...) をそのままネットワークスロットとして使用
        // (PlayerInventory/Hotbar 以外の場合)
        return containerSlotId
    }
}
