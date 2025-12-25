package org.infinite.libs.core.features

import org.infinite.utils.toLowerSnakeCase
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

abstract class FeatureCategories<
    CK : KClass<out Feature>,
    CV : Feature,
    K : KClass<out Category<CK, CV>>,
    V : Category<CK, CV>,
    > {
    abstract val categories: ConcurrentHashMap<K, V>

    fun data(): Map<String, Map<String, Map<String, Any?>>> {
        val data = mutableMapOf<String, Map<String, Map<String, Any?>>>()
        categories.values.forEach { category ->
            // Categoryのパッケージ名（最後から2番目）をIDとして使用
            val categoryId = category::class.qualifiedName?.split(".")?.let {
                if (it.size >= 2) it[it.size - 2].toLowerSnakeCase() else null
            } ?: "unknown"

            data[categoryId] = category.data()
        }
        return data
    }

    /**
     * 全カテゴリ、全Feature、全Propertyの翻訳キーを収集します。
     */
    val translations: List<String>
        get() = listOf(translationKey) + categories.values.flatMap { it.translations }

    /**
     * 最上位（features）または指定されたカテゴリ名の翻訳キーを取得します。
     */
    fun translation(name: String? = null): String? {
        if (name == null) return translationKey

        // カテゴリが存在するかチェック（パッケージ名またはクラス名で比較）
        val exists = categories.values.any { category ->
            val pkgName = category::class.qualifiedName?.split(".")?.let {
                if (it.size >= 2) it[it.size - 2] else null
            }
            pkgName?.toLowerSnakeCase() == name.toLowerSnakeCase()
        }

        return if (exists) "$translationKey.${name.toLowerSnakeCase()}" else null
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : V> insert(category: T) {
        categories[T::class as K] = category
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : V> getCategory(clazz: KClass<out T>): T? = categories[clazz as K] as? T

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
