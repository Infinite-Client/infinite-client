package org.infinite.libs.core.features.categories.category

import kotlinx.coroutines.*
import net.minecraft.client.DeltaTracker
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
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

    open suspend fun onStartUiRendering(deltaTracker: DeltaTracker): LinkedList<Pair<Int, List<RenderCommand>>> {
        return collectAndGroupRenderCommands(deltaTracker) { feature, graphics ->
            feature.onStartUiRendering(graphics)
            feature.renderPriority.start
        }
    }

    open suspend fun onEndUiRendering(deltaTracker: DeltaTracker): LinkedList<Pair<Int, List<RenderCommand>>> {
        return collectAndGroupRenderCommands(deltaTracker) { feature, graphics ->
            feature.onEndUiRendering(graphics)
            feature.renderPriority.end
        }
    }

    private suspend fun collectAndGroupRenderCommands(
        deltaTracker: DeltaTracker,
        block: (LocalFeature, Graphics2D) -> Int,
    ): LinkedList<Pair<Int, List<RenderCommand>>> = coroutineScope {
        val tempQueue = PriorityBlockingQueue<InternalCommandWrapper>(256, compareBy { it.priority })

        enabledFeatures().map { feature ->
            async(Dispatchers.Default) {
                val graphics2D = Graphics2D(deltaTracker)
                val priority = block(feature, graphics2D)
                val cmds = graphics2D.commands()
                if (cmds.isNotEmpty()) {
                    tempQueue.add(InternalCommandWrapper(priority, cmds.toList()))
                }
            }
        }.awaitAll()

        val result = LinkedList<Pair<Int, List<RenderCommand>>>()
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
        enabledFeatures().forEach {
            result.addAll(it.registerAllActions())
        }
        return result.toList()
    }

    private data class InternalCommandWrapper(val priority: Int, val commands: List<RenderCommand>)
}
