package org.infinite.libs.core.features.categories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.infinite.libs.core.features.FeatureCategories
import org.infinite.libs.core.features.categories.category.GlobalCategory
import org.infinite.libs.core.features.feature.GlobalFeature
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class GlobalFeatureCategories :
    FeatureCategories<KClass<out GlobalFeature>, GlobalFeature, KClass<out GlobalCategory>, GlobalCategory>() {
    override val categories: ConcurrentHashMap<KClass<out GlobalCategory>, GlobalCategory> = ConcurrentHashMap()

    // 初期化などのライフサイクルを管理するスコープ
    private val lifecycleScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun onInitialized() {
        lifecycleScope.launch {
            try {
                // すべてのカテゴリの初期化を並列実行
                categories.values
                    .map { category ->
                        launch { category.onInitialized() }
                    }.joinAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onShutdown() {
        // 1. まず、実行中の初期化処理などをすべてキャンセルする
        lifecycleScope.cancel()

        // 2. 終了処理（保存など）は「確実に終わるまで待つ」必要があるため runBlocking を使用
        runBlocking(Dispatchers.Default) {
            categories.values
                .map { category ->
                    launch {
                        try {
                            category.onShutdown()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }.joinAll()
        }
    }
}
