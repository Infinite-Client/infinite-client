package org.infinite.libs.core.features.categories.category

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.feature.GlobalFeature
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

open class GlobalCategory : Category<KClass<out GlobalFeature>, GlobalFeature>() {
    override val features: ConcurrentHashMap<KClass<out GlobalFeature>, GlobalFeature> = ConcurrentHashMap()

    suspend fun onInitialized() =
        coroutineScope {
            features.values
                .map { feature ->
                    launch(Dispatchers.Default) {
                        feature.onInitialized()
                    }
                }.joinAll()
        }

    suspend fun onShutdown() =
        coroutineScope {
            features.values
                .map { feature ->
                    launch(Dispatchers.Default) {
                        feature.onShutdown()
                    }
                }.joinAll()
        }

    suspend fun onStartTick() =
        coroutineScope {
            features.values
                .map { feature ->
                    launch(Dispatchers.Default) {
                        feature.onStartTick()
                    }
                }.joinAll()
        }

    suspend fun onEndTick() =
        coroutineScope {
            features.values
                .map { feature ->
                    launch(Dispatchers.Default) {
                        feature.onEndTick()
                    }
                }.joinAll()
        }
}
