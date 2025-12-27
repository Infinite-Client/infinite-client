package org.infinite.libs.core.features.categories.category

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.feature.GlobalFeature
import kotlin.reflect.KClass

/**
 * GlobalなFeatureを管理する抽象カテゴリ
 */
abstract class GlobalCategory : Category<KClass<out GlobalFeature>, GlobalFeature>() {

    // 委譲プロパティを使用するため、features の override は不要です。
    // 親クラス Category の val features: Map をそのまま使用します。

    open suspend fun onInitialized() = coroutineScope {
        features.values.map { launch(Dispatchers.Default) { it.onInitialized() } }.joinAll()
    }

    open suspend fun onShutdown() = coroutineScope {
        features.values.map { launch(Dispatchers.Default) { it.onShutdown() } }.joinAll()
    }

    open suspend fun onStartTick() = coroutineScope {
        features.values.map { launch(Dispatchers.Default) { it.onStartTick() } }.joinAll()
    }

    open suspend fun onEndTick() = coroutineScope {
        features.values.map { launch(Dispatchers.Default) { it.onEndTick() } }.joinAll()
    }
}
