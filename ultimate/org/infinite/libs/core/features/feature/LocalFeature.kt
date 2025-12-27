package org.infinite.libs.core.features.feature

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.infinite.libs.core.TickInterface
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.Graphics2D
import org.lwjgl.glfw.GLFW

open class LocalFeature : Feature(), TickInterface {
    open val defaultToggleKey: Int = GLFW.GLFW_KEY_UNKNOWN

    data class KeyAction(
        val name: String, // アクション名（翻訳キー等に使用）
        val defaultKey: Int, // デフォルトのキー
        val category: KeyMapping.Category,
        val action: () -> Unit, // 実行される処理
    )

    data class BindingPair(
        val mapping: KeyMapping,
        val action: () -> Unit,
    )

    private val registeredActions = mutableListOf<KeyAction>()
    fun registerAllActions(): List<BindingPair> {
        val parent = "key.${translation()}"
        val mappings = mutableListOf<BindingPair>()

        // 1. 定義された個別アクションの登録
        registeredActions.forEach { action ->
            val mapping = KeyBindingHelper.registerKeyBinding(
                KeyMapping(
                    "$parent.${action.name}",
                    action.defaultKey,
                    action.category,
                ),
            )
            mappings.add(
                BindingPair(
                    KeyBindingHelper.registerKeyBinding(mapping),
                    action.action,
                ),
            )
        }
        // 2. デフォルトのトグルキーの登録 (割り当てがある場合のみ、または常に)
        val toggleMapping = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "$parent.toggle",
                defaultToggleKey,
                KeyMapping.Category.GAMEPLAY,
            ),
        )
        mappings.add(
            BindingPair(
                toggleMapping,
            ) { toggle() },
        )
        return mappings.toList()
    }

    /**
     * 新しいキーアクションを登録する
     * @param name アクションの識別名
     * @param key デフォルトキーコード
     * @param action 実行する関数
     */
    fun defineAction(
        name: String,
        key: Int = GLFW.GLFW_KEY_UNKNOWN,
        category: KeyMapping.Category = KeyMapping.Category.GAMEPLAY,
        action: () -> Unit,
    ) {
        registeredActions.add(KeyAction(name, key, category, action))
    }

    open fun onConnected() {}
    open fun onDisconnected() {}
    override fun onStartTick() {}
    override fun onEndTick() {}
    data class RenderPriority(var start: Int, var end: Int)

    val renderPriority = RenderPriority(0, 0)
    open fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D = graphics2D
    open fun onEndUiRendering(graphics2D: Graphics2D): Graphics2D = graphics2D
}
