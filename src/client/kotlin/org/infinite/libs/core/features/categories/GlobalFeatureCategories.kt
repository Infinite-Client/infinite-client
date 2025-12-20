package org.infinite.libs.core.features.categories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.infinite.libs.core.features.FeatureCategories
import org.infinite.libs.core.features.categories.category.GlobalCategory
import org.infinite.libs.core.features.feature.GlobalFeature
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class GlobalFeatureCategories :
    FeatureCategories<KClass<out GlobalFeature>, GlobalFeature, KClass<out GlobalCategory>, GlobalCategory>() {
    override val categories: ConcurrentHashMap<KClass<out GlobalCategory>, GlobalCategory> = ConcurrentHashMap()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // coroutineScope を使用して、この関数自体をスコープにする
    fun onInitialized() {
        coroutineScope.launch {
            try {
                onInitializedSuspend()
            } catch (e: Exception) {
                // 初期化エラーのログ出力など
                e.printStackTrace()
            }
        }
    }

    private suspend fun onInitializedSuspend() =
        coroutineScope {
            categories.values
                .map { category ->
                    // この scope 内で launch する
                    launch(Dispatchers.Default) {
                        category.onInitialized()
                    }
                }.joinAll()
        }
}
