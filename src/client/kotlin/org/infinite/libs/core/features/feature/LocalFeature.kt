package org.infinite.libs.core.features.feature

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.infinite.libs.core.TickInterface
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.graphics.graphics3d.structs.RenderCommand3D
import org.lwjgl.glfw.GLFW

open class LocalFeature :
    Feature(),
    TickInterface {
    open val defaultToggleKey: Int = GLFW.GLFW_KEY_UNKNOWN

    enum class RenderUiTiming {
        Start,
        End,
    }

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

    // LocalFeature.kt 修正案
    fun registerAllActions(): List<BindingPair> {
        val parent = "key.${translation()}"
        val mappings = mutableListOf<BindingPair>()

        // 1. 個別アクションの登録
        registeredActions.forEach { action ->
            val mapping = KeyMapping(
                "$parent.${action.name}",
                action.defaultKey,
                action.category,
            )
            // registerKeyBinding は 1回だけ呼ぶ
            KeyBindingHelper.registerKeyBinding(mapping)
            val wrappedAction = {
                if (this.isEnabled()) action.action()
            }
            mappings.add(BindingPair(mapping, wrappedAction))
        }

        // 2. デフォルトのトグルキーの登録
        val toggleMapping = KeyMapping(
            "$parent.toggle",
            defaultToggleKey,
            KeyMapping.Category.GAMEPLAY,
        )
        KeyBindingHelper.registerKeyBinding(toggleMapping)
        mappings.add(BindingPair(toggleMapping) { toggle() })

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

    open fun onConnected() = Unit
    open fun onDisconnected() = Unit
    override fun onStartTick() = Unit
    override fun onEndTick() = Unit
    data class RenderPriority(var start: Int, var end: Int)

    fun handleRender2D(timing: RenderUiTiming): List<RenderCommand2D> {
        graphics2D.clear()
        when (timing) {
            RenderUiTiming.Start -> onStartUiRendering(graphics2D)
            RenderUiTiming.End -> onEndUiRendering(graphics2D)
        }
        return graphics2D.commands()
    }
    fun handleRender3D(): List<RenderCommand3D> {
        graphics3D.clear()
        onLevelRendering(graphics3D)
        return graphics3D.commands()
    }
    private val graphics3D = Graphics3D()
    private val graphics2D = Graphics2D()
    val renderPriority = RenderPriority(0, 0)
    protected open fun onStartUiRendering(graphics2D: Graphics2D) = Unit
    protected open fun onEndUiRendering(graphics2D: Graphics2D) = Unit
    protected open fun onLevelRendering(graphics3D: Graphics3D) = Unit
}
