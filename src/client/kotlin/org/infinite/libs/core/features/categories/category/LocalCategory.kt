package org.infinite.libs.core.features.categories.category

import kotlinx.coroutines.*
import net.minecraft.client.DeltaTracker
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import kotlin.reflect.KClass

open class LocalCategory : Category<KClass<out LocalFeature>, LocalFeature>() {
    override val features: ConcurrentHashMap<KClass<out LocalFeature>, LocalFeature> = ConcurrentHashMap()

    // 有効なFeatureのみをフィルタリングするヘルパー
    private fun enabledFeatures() = features.values.filter { it.isEnabled() }

    suspend fun onConnected() = coroutineScope {
        enabledFeatures().map { launch(Dispatchers.Default) { it.onConnected() } }.joinAll()
    }

    suspend fun onDisconnected() = coroutineScope {
        enabledFeatures().map { launch(Dispatchers.Default) { it.onDisconnected() } }.joinAll()
    }

    suspend fun onStartTick() = coroutineScope {
        enabledFeatures().map { launch(Dispatchers.Default) { it.onStartTick() } }.joinAll()
    }

    suspend fun onEndTick() = coroutineScope {
        enabledFeatures().map { launch(Dispatchers.Default) { it.onEndTick() } }.joinAll()
    }

    /**
     * UIレンダリングコマンドの収集と整理
     */
    suspend fun onStartUiRendering(deltaTracker: DeltaTracker): LinkedList<Pair<Int, List<RenderCommand>>> {
        return collectAndGroupRenderCommands(deltaTracker) { feature, graphics ->
            feature.onStartUiRendering(graphics)
            feature.renderPriority.start // start優先度を使用
        }
    }

    suspend fun onEndUiRendering(deltaTracker: DeltaTracker): LinkedList<Pair<Int, List<RenderCommand>>> {
        return collectAndGroupRenderCommands(deltaTracker) { feature, graphics ->
            feature.onEndUiRendering(graphics)
            feature.renderPriority.end // end優先度を使用
        }
    }

    /**
     * 共通ロジック：Featureからコマンドを集め、優先度ごとにグループ化する
     */
    private suspend fun collectAndGroupRenderCommands(
        deltaTracker: DeltaTracker,
        block: (LocalFeature, Graphics2D) -> Int,
    ): LinkedList<Pair<Int, List<RenderCommand>>> {
        // 1. 各Featureからコマンドを並列収集 (zIndexとしてpriorityを保持)
        val tempQueue = PriorityBlockingQueue<InternalCommandWrapper>(256, compareBy { it.priority })

        coroutineScope {
            enabledFeatures().map { feature ->
                async(Dispatchers.Default) {
                    val graphics2D = Graphics2D(deltaTracker)
                    val priority = block(feature, graphics2D)

                    val commands = mutableListOf<RenderCommand>()
                    commands.addAll(graphics2D.commands())
                    if (commands.isNotEmpty()) {
                        tempQueue.add(InternalCommandWrapper(priority, commands))
                    }
                }
            }.awaitAll()
        }

        // 2. 優先度ごとにグループ化して LinkedList に変換
        val result = LinkedList<Pair<Int, List<RenderCommand>>>()

        while (tempQueue.isNotEmpty()) {
            val wrapper = tempQueue.poll() ?: break
            // 同じ優先度のものが既に最後尾にあれば結合、なければ新規追加
            if (result.isNotEmpty() && result.last().first == wrapper.priority) {
                val lastEntry = result.removeLast()
                result.add(lastEntry.first to (lastEntry.second + wrapper.commands))
            } else {
                result.add(wrapper.priority to wrapper.commands)
            }
        }

        return result
    }

    // 内部ソート用のラッパー
    private data class InternalCommandWrapper(
        val priority: Int,
        val commands: List<RenderCommand>,
    )
}
