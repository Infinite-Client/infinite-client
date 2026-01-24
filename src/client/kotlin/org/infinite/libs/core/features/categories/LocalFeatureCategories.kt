package org.infinite.libs.core.features.categories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.client.DeltaTracker
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.FeatureCategories
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.graphics.graphics3d.structs.RenderCommand3D
import org.infinite.libs.translation.TranslationChecker
import java.util.*
import kotlin.reflect.KClass

/**
 * Local（ワールド/サーバー接続中のみ生存）なカテゴリー管理の基底クラス
 */
abstract class LocalFeatureCategories :
    FeatureCategories<
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

    suspend fun onStartUiRendering(deltaTracker: DeltaTracker): List<RenderCommand2D> = mergeCategoriesRendering { it.onStartUiRendering(deltaTracker) }

    suspend fun onEndUiRendering(deltaTracker: DeltaTracker): List<RenderCommand2D> = mergeCategoriesRendering { it.onEndUiRendering(deltaTracker) }

    private suspend fun mergeCategoriesRendering(
        fetchBlock: suspend (LocalCategory) -> LinkedList<Pair<Int, List<RenderCommand2D>>>,
    ): List<RenderCommand2D> = coroutineScope {
        // 1. 各カテゴリーから並列取得
        val deferredResults = categories.values.map { category ->
            async(Dispatchers.Default) { fetchBlock(category) }
        }.awaitAll()

        // 2. Priority順に自動ソートされる TreeMap で統合
        val sortedMap = TreeMap<Int, MutableList<RenderCommand2D>>()
        for (categoryResult in deferredResults) {
            for ((priority, commands) in categoryResult) {
                sortedMap.getOrPut(priority) { mutableListOf() }.addAll(commands)
            }
        }

        // 3. フラット化
        val finalCommands = mutableListOf<RenderCommand2D>()
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
            InfiniteClient.gameScreenBindingPair,
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

    suspend fun onLevelRendering(): List<RenderCommand3D> = coroutineScope {
        // 1. 各カテゴリーから並列にコマンドを回収
        val deferredResults = categories.values.map { category ->
            async(Dispatchers.Default) {
                category.onLevelRendering()
            }
        }

        // 2. 全ての結果を待機し、一つのリストに統合
        deferredResults.awaitAll().flatten()
    }
}
