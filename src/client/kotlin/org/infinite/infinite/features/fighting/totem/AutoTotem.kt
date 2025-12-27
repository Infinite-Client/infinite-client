package org.infinite.infinite.features.fighting.totem

import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.Items
import org.infinite.InfiniteClient
import org.infinite.infinite.features.utils.backpack.BackPackManager
import org.infinite.libs.client.async.AsyncInterface
import org.infinite.libs.client.inventory.InventoryManager
import org.infinite.libs.client.inventory.InventoryManager.InventoryIndex
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting

class AutoTotem : ConfigurableFeature(initialEnabled = false) {
    private val hpSetting: FeatureSetting.IntSetting =
        FeatureSetting.IntSetting("Hp", 10, 1, 20)

    // アンチチート回避のためのランダム遅延の最小値（ミリ秒）
    private val minDelaySetting: FeatureSetting.IntSetting =
        FeatureSetting.IntSetting("MinDelayMs", 60, 0, 500)

    // アンチチート回避のためのランダム遅延の最大値（ミリ秒）
    private val maxDelaySetting: FeatureSetting.IntSetting =
        FeatureSetting.IntSetting("MaxDelayMs", 180, 0, 500)

    override val settings: List<FeatureSetting<*>> =
        listOf(
            hpSetting,
            minDelaySetting,
            maxDelaySetting,
        )

    // 現在、トーテム装備シーケンスが進行中かどうかを示すフラグ
    private var isSwapping: Boolean = false

    override fun onTick() {
        val health: Float = player?.health ?: 0f
        val targetHealth = hpSetting.value
        val manager = InventoryManager

        // 1. ヘルスチェックと、操作進行中のチェック
        if (health < targetHealth && !isSwapping) {
            val currentItem = manager.get(InventoryIndex.OffHand())

            // 2. オフハンドにトーテムがないことを確認
            if (currentItem.item != Items.TOTEM_OF_UNDYING) {
                val targetSlot = manager.findFirst(Items.TOTEM_OF_UNDYING)

                // 3. インベントリにトーテムがあることを確認
                if (targetSlot != null) {
                    // 操作シーケンスを開始し、フラグを立てる
                    isSwapping = true

                    // 🌟 ランダム遅延を含むスワップ操作の予約
                    scheduleDelayedSwap(manager, targetSlot)
                }
            }
        } else if (health >= targetHealth) {
            // ヘルスが安全域に戻ったら、フラグをリセット
            isSwapping = false
        }
    }

    // AutoTotem操作シーケンスをAsyncInterfaceに予約するプライベート関数
    private fun scheduleDelayedSwap(
        manager: InventoryManager,
        targetSlot: InventoryIndex,
    ) {
        val currentPlayer = player ?: return
        val interactionManager = interactionManager // InteractionManagerの取得を仮定
        val backPackManager = InfiniteClient.getFeature(BackPackManager::class.java)

        // スロットの変換
        val netA = InventoryIndex.OffHand().slotId() ?: return
        val netB = targetSlot.slotId() ?: return
        val currentScreenId = currentPlayer.containerMenu.containerId

        // ランダムなディレイを計算するヘルパー
        fun randomDelay(): Long {
            val min = minDelaySetting.value
            val max = maxDelaySetting.value
            return (min..max).random().toLong()
        }

        var cumulativeDelay = 0L
        // ★ BackPackManagerの一時停止/再開をregisterで置き換え
        backPackManager?.register {
            // --- 1. クリック 1 (オフハンドを掴む) ---
            // 最初のクリックは、操作の開始として即座に予約（遅延0）
            AsyncInterface.add(
                AsyncInterface.AsyncAction(0L) {
                    interactionManager?.handleInventoryMouseClick(currentScreenId, netA, 0, ClickType.PICKUP, currentPlayer)
                },
            )

            // --- 2. クリック 2 (トーテムスロットに置く) ---
            // ランダム遅延 1 を計算
            val delay1 = randomDelay()
            cumulativeDelay += delay1
            AsyncInterface.add(
                AsyncInterface.AsyncAction(cumulativeDelay) {
                    interactionManager?.handleInventoryMouseClick(currentScreenId, netB, 0, ClickType.PICKUP, currentPlayer)
                },
            )

            // --- 3. クリック 3 (オフハンドを再度掴む) ---
            // ランダム遅延 2 を計算
            val delay2 = randomDelay()
            cumulativeDelay += delay2
            AsyncInterface.add(
                AsyncInterface.AsyncAction(cumulativeDelay) {
                    interactionManager?.handleInventoryMouseClick(currentScreenId, netA, 0, ClickType.PICKUP, currentPlayer)
                },
            )

            // --- 4. 修正ロジックの開始 (カーソルアイテムの処理) ---
            // ランダム遅延 3 を計算 (3クリック後のカーソル処理に移るまでの間隔)
            val delay3 = randomDelay()
            cumulativeDelay += delay3
            AsyncInterface.add(
                AsyncInterface.AsyncAction(cumulativeDelay) {
                    // カーソルにアイテムが残っているかチェック
                    if (!currentPlayer.containerMenu.carried.isEmpty) {
                        val emptyBackpackSlot = manager.findFirstEmptyBackpackSlot()

                        if (emptyBackpackSlot != null) {
                            // 空きスロットが見つかった場合、そこに配置する操作を予約
                            val emptyNetSlot = emptyBackpackSlot.slotId() ?: return@AsyncAction

                            // 5. クリック 4 (カーソルアイテムを空きスロットに戻す)
                            // 極短い遅延（例: 20ms）後に実行を予約し、パケット間のばらつきを維持
                            val delay4 = (0L..20L).random()
                            cumulativeDelay += delay4

                            AsyncInterface.add(
                                AsyncInterface.AsyncAction(cumulativeDelay) {
                                    interactionManager?.handleInventoryMouseClick(
                                        currentScreenId,
                                        emptyNetSlot,
                                        0,
                                        ClickType.PICKUP,
                                        currentPlayer,
                                    )
                                    isSwapping = false // すべての操作が完了
                                },
                            )
                        } else {
                            // 5. カーソルアイテムをドロップ (空きスロットがない場合)
                            val delay4 = (0L..20L).random()
                            cumulativeDelay += delay4

                            AsyncInterface.add(
                                AsyncInterface.AsyncAction(cumulativeDelay) {
                                    interactionManager?.handleInventoryMouseClick(currentScreenId, -999, 0, ClickType.PICKUP, currentPlayer)
                                    isSwapping = false // すべての操作が完了
                                },
                            )
                        }
                    } else {
                        // カーソルにアイテムが残っていなければ、即座に完了
                        isSwapping = false
                    }
                },
            )
        }
    }
}
