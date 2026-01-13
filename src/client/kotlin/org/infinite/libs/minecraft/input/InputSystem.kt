package org.infinite.libs.minecraft.input

import net.minecraft.client.KeyMapping
import java.util.concurrent.ConcurrentLinkedDeque

@Suppress("Unused")
object InputSystem {
    enum class KeyActionKind {
        Press,
        Release,
    }

    class KeyAction(
        val key: KeyMapping,
        val kind: KeyActionKind,
        initialTicks: Int,
        val condition: () -> Boolean,
    ) {
        var remainingTicks: Int = initialTicks

        // KeyMappingとKeyActionKindに基づく一意のID
        val actionId: String = key.name

        /**
         * アクションを実行し、残りのティック数を減らす。
         * 戻り値: アクションをリストから削除すべきか (true)、継続すべきか (false)
         */
        fun tick(): Boolean {
            // 条件を満たさない場合、即座に終了
            if (!condition()) {
                // Pressアクションの場合は、状態をリセット
                if (kind == KeyActionKind.Press) {
                    key.isDown = false
                }
                return true // 削除
            }

            when (kind) {
                KeyActionKind.Press -> key.isDown = true
                KeyActionKind.Release -> key.isDown = false
            }

            // ティック数が 0 になったら終了処理を行う
            if (remainingTicks <= 0) {
                // Pressアクションの終了時、キーの状態をリセット
                if (kind == KeyActionKind.Press) {
                    key.isDown = false
                }
                return true
            }
            remainingTicks--
            return false // 継続
        }
    }

    // ConcurrentLinkedDeque を使用し、マルチスレッド環境での安全性と高速な操作性を実現
    private var keyActionList: ConcurrentLinkedDeque<KeyAction> = ConcurrentLinkedDeque()

    /**
     * 新しい KeyAction をリストに追加する前に、同じ KeyMapping と KeyActionKind を持つ
     * 既存のアクションをすべて削除するヘルパー関数。
     */
    private fun addActionWithOverride(newAction: KeyAction) {
        // 同種のアクションを検索して削除 (上書きロジック)
        keyActionList.removeIf { existingAction ->
            existingAction.actionId == newAction.actionId
        }
        // 新しいアクションを追加
        keyActionList.add(newAction)
    }

    // --- Public Functions ---

    /**
     * 指定したキーをtick数分、またはconditionがfalseになるまで押すアクションを追加します。
     */
    fun press(
        key: KeyMapping,
        tick: Int = 1,
        condition: () -> Boolean = { true },
    ) {
        val newAction = KeyAction(key, KeyActionKind.Press, tick.coerceAtLeast(1), condition)
        addActionWithOverride(newAction)
    }

    /**
     * 指定したキーをconditionがfalseになるまで押すアクションを追加します (tick数は実質無限: Int.MAX_VALUE)。
     */
    fun press(
        key: KeyMapping,
        condition: () -> Boolean,
    ) = press(key, Int.MAX_VALUE, condition)

    /**
     * 指定したキーをtick数分、またはconditionがfalseになるまで離すアクションを追加します。
     */
    fun release(
        key: KeyMapping,
        tick: Int = 1,
        condition: () -> Boolean = { true },
    ) {
        val newAction = KeyAction(key, KeyActionKind.Release, tick.coerceAtLeast(1), condition)
        addActionWithOverride(newAction)
    }

    /**
     * 指定したキーをconditionがfalseになるまで離すアクションを追加します (tick数は実質無限)。
     */
    fun release(
        key: KeyMapping,
        condition: () -> Boolean,
    ) = release(key, Int.MAX_VALUE, condition)

    /**
     * キュー内のすべてのアクションを実行します。
     * 実行が完了したアクション、または条件を満たさなくなったアクションは削除します。
     */
    fun tick() {
        val iterator = keyActionList.iterator()
        while (iterator.hasNext()) {
            val action = iterator.next()
            if (action.tick()) {
                iterator.remove()
            }
        }
    }
}
