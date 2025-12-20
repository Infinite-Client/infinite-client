package org.infinite.libs.core.features.categories.category

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.feature.LocalFeature
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

open class LocalCategory : Category<KClass<out LocalFeature>, LocalFeature>() {
    override val features: ConcurrentHashMap<KClass<out LocalFeature>, LocalFeature> = ConcurrentHashMap()

    /**
     * サーバー接続時：配下の全 LocalFeature を並列初期化
     */
    suspend fun onConnected() =
        coroutineScope {
            features.values
                .map { feature ->
                    launch(Dispatchers.Default) {
                        feature.onConnected()
                    }
                }.joinAll()
        }

    /**
     * サーバー切断時：配下の全 LocalFeature の終了処理を並列実行
     */
    suspend fun onDisconnected() =
        coroutineScope {
            features.values
                .map { feature ->
                    launch(Dispatchers.Default) {
                        feature.onDisconnected()
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
