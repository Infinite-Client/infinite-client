package org.infinite.libs.core.features

import org.infinite.utils.toLowerSnakeCase
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FeatureCategories<
    CK : KClass<out Feature>,
    CV : Feature,
    K : KClass<out Category<CK, CV>>,
    V : Category<CK, CV>,
    > {
    // 外部からは Map として公開し、内部で書き込む
    private val _categories = ConcurrentHashMap<K, V>()
    val categories: Map<K, V> get() = _categories

    /**
     * Categoryを定義するための委譲プロバイダー
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T : V> category(category: T): CategoryDelegate<T> {
        // T::class をキーにしてマップに登録
        _categories[category::class as K] = category
        return CategoryDelegate(category)
    }

    protected inner class CategoryDelegate<T : V>(val category: T) {
        operator fun getValue(thisRef: FeatureCategories<CK, CV, K, V>, prop: KProperty<*>): T = category
    }

    // --- 既存のメソッドの調整 (categories -> _categories に変更) ---

    fun data(): Map<String, Map<String, Feature.FeatureData>> {
        val data = mutableMapOf<String, Map<String, Feature.FeatureData>>()
        _categories.values.forEach { category ->
            val categoryId = category::class.qualifiedName?.split(".")?.let {
                if (it.size >= 2) it[it.size - 2].toLowerSnakeCase() else null
            } ?: "unknown"
            data[categoryId] = category.data()
        }
        return data
    }

    val translations: List<String>
        get() = listOf(translationKey) + _categories.values.flatMap { it.translations }

    fun translation(name: String? = null): String? {
        if (name == null) return translationKey
        val exists = _categories.values.any { category ->
            val pkgName = category::class.qualifiedName?.split(".")?.let {
                if (it.size >= 2) it[it.size - 2] else null
            }
            pkgName?.toLowerSnakeCase() == name.toLowerSnakeCase()
        }
        return if (exists) "$translationKey.${name.toLowerSnakeCase()}" else null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : V> getCategory(category: KClass<out T>): T? = _categories[category as K] as? T

    /**
     * 例: ultimate.features.local
     */
    private val translationKey: String by lazy {
        val modId = "ultimate"
        val translationCategory = "features"
        val fullName = this::class.qualifiedName
            ?: throw IllegalArgumentException("Qualified name not found")
        val parts = fullName.split(".")

        // FeatureCategories が org.infinite.features.local.LocalFeatureCategories なら
        // size-2 の "local" をスコープとして取得
        if (parts.size >= 2) {
            val scope = parts[parts.size - 2].toLowerSnakeCase()
            "$modId.$translationCategory.$scope"
        } else {
            "$modId.$translationCategory"
        }
    }
}
