package org.infinite.libs.core.features

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

// Categoryは現在 Category<K, V> と定義されているため、
// FeatureCategoriesでもそれを受け取って渡す必要があります
abstract class FeatureCategories<
    CK : KClass<out Feature>,
    CV : Feature,
    K : KClass<out Category<CK, CV>>,
    V : Category<CK, CV>,
> {
    abstract val categories: ConcurrentHashMap<K, V>

    /**
     * カテゴリを登録する
     * 具体的な型（T）を LocalCategory として適切にキャストして Map に格納します
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : V> insert(category: T) {
        categories[T::class as K] = category
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : V> getCategory(clazz: KClass<out T>): T? = categories[clazz as K] as? T
}
