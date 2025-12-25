package org.infinite.libs.core.features

import org.infinite.utils.toLowerSnakeCase
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

abstract class Category<K : KClass<out Feature>, V : Feature> {
    abstract val features: ConcurrentHashMap<K, V>

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : V> insert(feature: T) {
        features[T::class as K] = feature
    }

    /**
     * 指定された名前の翻訳キーを取得します。
     * @param name null の場合はこのカテゴリ自身のキー、
     * Feature のクラス名（SimpleName）などが指定された場合はその Feature のキーを返します。
     */
    fun translation(name: String? = null): String? {
        if (name == null) return translationKey

        // name が指定された場合、その名前を持つ Feature がこのカテゴリ内に存在するか確認
        // Feature の simpleName は通常 PascalCase なので、比較のために変換ロジックを考慮
        val featureExists = features.values.any {
            it::class.simpleName == name || it::class.simpleName?.toLowerSnakeCase() == name.toLowerSnakeCase()
        }
        return if (featureExists) {
            "$translationKey.${name.toLowerSnakeCase()}"
        } else {
            null
        }
    }

    /**
     * このカテゴリに属するすべての Feature の翻訳キーをリストで取得します。
     */
    val translations: List<String>
        get() = listOf(translationKey) + features.values.map { it.translation()!! }

    fun data(): Map<String, Map<String, Any?>> {
        val data = mutableMapOf<String, Map<String, Any?>>()
        features.values.forEach { feature ->
            val featureId = feature::class.simpleName?.toLowerSnakeCase()
                ?: throw IllegalStateException("Feature class name not found")
            data[featureId] = feature.data()
        }
        return data
    }

    private val translationKey: String by lazy {
        val modId = "ultimate"
        val translationCategory = "features"
        val fullName = this::class.qualifiedName
            ?: throw IllegalArgumentException("Qualified name not found for ${this::class.simpleName}")
        val parts = fullName.split(".")
        val size = parts.size

        // Category のパッケージ構造が org.infinite.features.local.RenderingCategory だと仮定
        // size-1: RenderingCategory, size-2: rendering, size-3: local
        if (size >= 3) {
            val category = parts[size - 2].toLowerSnakeCase()
            val scope = parts[size - 3].toLowerSnakeCase()
            "$modId.$translationCategory.$scope.$category"
        } else {
            throw IllegalArgumentException("Package hierarchy is too shallow: $fullName")
        }
    }
}
