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

    open suspend fun onStartUiRendering(deltaTracker: DeltaTracker): LinkedList<Pair<Int, List<RenderCommand2D>>> = collectAndGroupRenderCommands(LocalFeature.RenderUiTiming.Start)

    open suspend fun onEndUiRendering(deltaTracker: DeltaTracker): LinkedList<Pair<Int, List<RenderCommand2D>>> = collectAndGroupRenderCommands(LocalFeature.RenderUiTiming.End)

    open suspend fun onLevelRendering(): List<RenderCommand3D> = coroutineScope {
        // 各FeatureのhandleRender3Dを並列実行
        val deferredCommands = enabledFeatures().map { feature ->
            async(Dispatchers.Default) {
                feature.handleRender3D()
            }
        }
        // 全ての結果をマージ
        deferredCommands.awaitAll().flatten()
    }

    private suspend fun collectAndGroupRenderCommands(
        timing: LocalFeature.RenderUiTiming,
    ): LinkedList<Pair<Int, List<RenderCommand2D>>> = coroutineScope {
        val tempQueue = PriorityBlockingQueue<InternalCommandWrapper>(256, compareBy { it.priority })

        enabledFeatures().map { feature ->
            async(Dispatchers.Default) {
                // Feature側の新しいメソッドを利用
                val cmd = feature.handleRender2D(timing)

                if (cmd.isNotEmpty()) {
                    val priority = if (timing == LocalFeature.RenderUiTiming.Start) {
                        feature.renderPriority.start
                    } else {
                        feature.renderPriority.end
                    }

                    tempQueue.add(InternalCommandWrapper(priority, cmd))
                }
            }
        }.awaitAll()

        // 優先度順にグルーピング
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

    fun registerAllActions(): List<LocalFeature.BindingPair> = features.values.flatMap { it.registerAllActions() }

    private data class InternalCommandWrapper(val priority: Int, val commands: List<RenderCommand2D>)
}
