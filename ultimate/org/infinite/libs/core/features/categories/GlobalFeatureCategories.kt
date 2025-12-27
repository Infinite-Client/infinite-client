package org.infinite.libs.core.features.categories

import kotlinx.coroutines.*
import org.infinite.libs.core.features.FeatureCategories
import org.infinite.libs.core.features.categories.category.GlobalCategory
import org.infinite.libs.core.features.feature.GlobalFeature
import kotlin.reflect.KClass

/**
 * Global（クライアント起動から終了まで生存）なカテゴリー管理の基底クラス
 */
abstract class GlobalFeatureCategories : FeatureCategories<
    KClass<out GlobalFeature>,
    GlobalFeature,
    KClass<out GlobalCategory>,
    GlobalCategory,
    >() {

    // ライフサイクル管理用のスコープ
    protected val lifecycleScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun onInitialized() {
        lifecycleScope.launch {
            categories.values.map { launch { it.onInitialized() } }.joinAll()
        }
    }

    fun onShutdown() {
        lifecycleScope.cancel()
        runBlocking(Dispatchers.Default) {
            categories.values.map { launch { it.onShutdown() } }.joinAll()
        }
    }

    fun onStartTick() {
        lifecycleScope.launch {
            categories.values.map { launch { it.onStartTick() } }.joinAll()
        }
    }

    fun onEndTick() {
        lifecycleScope.launch {
            categories.values.map { launch { it.onEndTick() } }.joinAll()
        }
    }
}
