package org.infinite.libs.core.features.categories.category

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import net.minecraft.client.DeltaTracker
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.graphics.graphics3d.structs.RenderCommand3D
import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import kotlin.reflect.KClass

/**
 * LocalなFeatureを管理する抽象カテゴリ
 */
abstract class LocalCategory : Category<KClass<out LocalFeature>, LocalFeature>() {

    // 有効なFeatureのみをフィルタリング
    private fun enabledFeatures() = features.values.filter { it.isEnabled() }

    open suspend fun onConnected() = coroutineScope {
        enabledFeatures().map { launch(Dispatchers.Default) { it.onConnected() } }.joinAll()
    }

    open suspend fun onDisconnected() = coroutineScope {
        enabledFeatures().map { launch(Dispatchers.Default) { it.onDisconnected() } }.joinAll()
    }

    open suspend fun onStartTick() = coroutineScope {
        enabledFeatures().map { launch(Dispatchers.Default) { it.onStartTick() } }.joinAll()
    }

    open suspend fun onEndTick() = coroutineScope {
        enabledFeatures().map { launch(Dispatchers.Default) { it.onEndTick() } }.joinAll()
    }

    // --- Rendering Logic ---

    open suspend fun onStartUiRendering(deltaTracker: DeltaTracker): LinkedList<Pair<Int, List<RenderCommand2D>>> = collectAndGroupRenderCommands(deltaTracker) { feature, graphics ->
        feature.onStartUiRendering(graphics)
        feature.renderPriority.start
    }

    open suspend fun onEndUiRendering(deltaTracker: DeltaTracker): LinkedList<Pair<Int, List<RenderCommand2D>>> = collectAndGroupRenderCommands(deltaTracker) { feature, graphics ->
        feature.onEndUiRendering(graphics)
        feature.renderPriority.end
    }
    open suspend fun onLevelRendering(): List<RenderCommand3D> = coroutineScope {
        // 有効な Feature ごとに並列でコマンドを生成
        val deferredCommands = enabledFeatures().map { feature ->
            async(Dispatchers.Default) {
                val graphics3D = Graphics3D()
                feature.onLevelRendering(graphics3D)
                graphics3D.commands()
            }
        }
        // 全ての Feature からの結果を待ち、一つのリストに統合して返す
        deferredCommands.awaitAll().flatten()
    }

    private suspend fun collectAndGroupRenderCommands(
        deltaTracker: DeltaTracker,
        block: (LocalFeature, Graphics2D) -> Int,
    ): LinkedList<Pair<Int, List<RenderCommand2D>>> = coroutineScope {
        val tempQueue = PriorityBlockingQueue<InternalCommandWrapper>(256, compareBy { it.priority })

        enabledFeatures().map { feature ->
            async(Dispatchers.Default) {
                val graphics2D = Graphics2D(deltaTracker)
                val priority = block(feature, graphics2D)
                val cmd = graphics2D.commands()
                if (cmd.isNotEmpty()) {
                    tempQueue.add(InternalCommandWrapper(priority, cmd.toList()))
                }
            }
        }.awaitAll()

        val result = LinkedList<Pair<Int, List<RenderCommand2D>>>()
        while (tempQueue.isNotEmpty()) {
            val wrapper = tempQueue.poll() ?: break
            if (result.isNotEmpty() && result.last().first == wrapper.priority) {
                val lastEntry = result.removeLast()
                result.add(lastEntry.first to (lastEntry.second + wrapper.commands))
            } else {
                result.add(wrapper.priority to wrapper.commands)
            }
        }
        result
    }

    fun registerAllActions(): List<LocalFeature.BindingPair> {
        val result = mutableListOf<LocalFeature.BindingPair>()
        features.values.forEach {
            result.addAll(it.registerAllActions())
        }
        return result.toList()
    }

    private data class InternalCommandWrapper(val priority: Int, val commands: List<RenderCommand2D>)
}
