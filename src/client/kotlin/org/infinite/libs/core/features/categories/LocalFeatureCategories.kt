package org.infinite.libs.core.features.categories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.infinite.features.local.rendering.LocalRenderingCategory
import org.infinite.libs.core.features.FeatureCategories
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.core.features.feature.LocalFeature
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class LocalFeatureCategories : FeatureCategories<KClass<out LocalFeature>, LocalFeature, KClass<out LocalCategory>, LocalCategory>() {
    override val categories: ConcurrentHashMap<KClass<out LocalCategory>, LocalCategory> = ConcurrentHashMap()

    // 接続ごとに作り直すためのスコープ。初期値は null または空のスコープ
    private var connectionScope: CoroutineScope? = null

    init {
        insert(LocalRenderingCategory())
    }

    /**
     * サーバー接続時の処理（非同期・並列）
     */
    fun onConnected() {
        // 1. もし前の接続が残っていたらキャンセルして掃除する
        connectionScope?.cancel()

        // 2. 新しい接続用のスコープを作成
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        connectionScope = scope

        // 3. 並列で初期化を実行
        scope.launch {
            try {
                categories.values
                    .map { category ->
                        launch { category.onConnected() }
                    }.joinAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * サーバー切断時の処理（同期待機）
     */
    fun onDisconnected() {
        // 1. 現在進行中の処理（onConnectedなど）をすべて即座に止める
        connectionScope?.cancel()
        connectionScope = null

        // 2. 終了処理を確実に終わらせるために runBlocking を使用
        runBlocking(Dispatchers.Default) {
            categories.values
                .map { category ->
                    launch {
                        try {
                            category.onDisconnected()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }.joinAll()
        }
    }

    /**
     * マイクラ終了時
     */
    fun onShutdown() {
        onDisconnected()
    }
}
