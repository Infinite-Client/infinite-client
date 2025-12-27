package org.infinite.libs.core.features.categories

import kotlinx.coroutines.*
import net.minecraft.client.DeltaTracker
import org.infinite.UltimateClient
import org.infinite.libs.core.features.FeatureCategories
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.translation.TranslationChecker
import java.util.*
import kotlin.reflect.KClass

/**
 * Local（ワールド/サーバー接続中のみ生存）なカテゴリー管理の基底クラス
 */
abstract class LocalFeatureCategories : FeatureCategories<
    KClass<out LocalFeature>,
    LocalFeature,
    KClass<out LocalCategory>,
    LocalCategory,
    >() {

    private var connectionScope: CoroutineScope? = null

    // --- 接続ライフサイクル ---

    fun onConnected() {
        connectionScope?.cancel()
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        connectionScope = scope
        scope.launch {
            categories.values.map { launch { it.onConnected() } }.joinAll()
        }
    }

    fun onDisconnected() {
        connectionScope?.cancel()
        connectionScope = null
        runBlocking(Dispatchers.Default) {
            categories.values.map { launch { it.onDisconnected() } }.joinAll()
        }
    }

    fun onShutdown() = onDisconnected()

    // --- Tick ---

    fun onStartTick() {
        connectionScope?.launch {
            categories.values.map { launch { it.onStartTick() } }.joinAll()
        }
    }

    fun onEndTick() {
        connectionScope?.launch {
            categories.values.map { launch { it.onEndTick() } }.joinAll()
        }
    }

    // --- レンダリング統合ロジック ---

    suspend fun onStartUiRendering(deltaTracker: DeltaTracker): List<RenderCommand> {
        return mergeCategoriesRendering { it.onStartUiRendering(deltaTracker) }
    }

    suspend fun onEndUiRendering(deltaTracker: DeltaTracker): List<RenderCommand> {
        return mergeCategoriesRendering { it.onEndUiRendering(deltaTracker) }
    }

    private suspend fun mergeCategoriesRendering(
        fetchBlock: suspend (LocalCategory) -> LinkedList<Pair<Int, List<RenderCommand>>>,
    ): List<RenderCommand> = coroutineScope {
        // 1. 各カテゴリーから並列取得
        val deferredResults = categories.values.map { category ->
            async(Dispatchers.Default) { fetchBlock(category) }
        }.awaitAll()

        // 2. Priority順に自動ソートされる TreeMap で統合
        val sortedMap = TreeMap<Int, MutableList<RenderCommand>>()
        for (categoryResult in deferredResults) {
            for ((priority, commands) in categoryResult) {
                sortedMap.getOrPut(priority) { mutableListOf() }.addAll(commands)
            }
        }

        // 3. フラット化
        val finalCommands = mutableListOf<RenderCommand>()
        for (commandsInPriority in sortedMap.values) {
            finalCommands.addAll(commandsInPriority)
        }
        finalCommands
    }

    lateinit var keybindingPairs: List<LocalFeature.BindingPair>
    fun registerAllActions() {
        val result = mutableListOf<LocalFeature.BindingPair>()
        categories.values.forEach {
            result.addAll(it.registerAllActions())
        }
        result.add(
            UltimateClient.gameScreenBindingPair,
        )
        result.forEach { TranslationChecker.add(it.mapping.name) }
        keybindingPairs = result.toList()
    }

    fun keyBindingActions() {
        if (!::keybindingPairs.isInitialized) {
            return
        }
        if (keybindingPairs.isEmpty()) return
        keybindingPairs.forEach { pair ->
            while (pair.mapping.consumeClick()) {
                pair.action()
            }
        }
    }
}
