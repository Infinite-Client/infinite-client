package org.infinite.libs.core.features

import org.infinite.utils.toLowerSnakeCase
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Category<K : KClass<out Feature>, V : Feature> {
    // 外部からは読み取り専用、内部で PropertyDelegate から書き込み
// クラス名のアルファベット順でソートする Comparator を定義
    private val comparator = Comparator<K> { k1, k2 ->
        val name1 = k1.simpleName ?: ""
        val name2 = k2.simpleName ?: ""
        name1.compareTo(name2)
    }

    // スレッドセーフかつソートされたMapを作成
    // synchronizedMap でラップすることで、ConcurrentHashMapに近いスレッド安全性を確保します
    private val _features: MutableMap<K, V> = Collections.synchronizedMap(TreeMap<K, V>(comparator))

    val features: Map<K, V> get() = _features
    open val name: String = this::class.simpleName ?: "UnknownCategory"

    /**
     * Featureを定義するための委譲プロバイダー
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T : V> feature(feature: T): FeatureDelegate<T> {
        return FeatureDelegate(feature)
    }

    protected inner class FeatureDelegate<T : V>(val feature: T) {
        operator fun provideDelegate(thisRef: Category<K, V>, prop: KProperty<*>): FeatureDelegate<T> {
            // マップへの登録 (KClassをキーにする)
            @Suppress("UNCHECKED_CAST")
            _features[feature::class as K] = feature
            return this
        }

        operator fun getValue(thisRef: Category<K, V>, prop: KProperty<*>): T = feature
    }

    // --- 既存のメソッド (一部調整) ---

    @Suppress("UNCHECKED_CAST")
    fun <T : V> getFeature(feature: KClass<out T>): T? = _features[feature as K] as? T

    fun data(): Map<String, Feature.FeatureData> = _features.values.associate { feature ->
        val featureId = feature::class.simpleName?.toLowerSnakeCase()
            ?: throw IllegalStateException("Name not found")
        featureId to feature.data()
    }

    /**
     * 指定された名前の翻訳キーを取得します。
     * @param name null の場合はこのカテゴリ自身のキー、
     * Feature のクラス名（SimpleName）などが指定された場合はその Feature のキーを返します。
     */
    fun translation(name: String): String? {
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

    fun translation(): String = translationKey

    /**
     * このカテゴリに属するすべての Feature の翻訳キーをリストで取得します。
     */
    val translations: List<String>
        get() = listOf(translationKey) + features.values.flatMap { it.translations() }

    private val translationKey: String by lazy {
        val modId = "infinite"
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

    fun reset() {
        _features.values.forEach { it.reset() }
    }

    fun <X> map(action: (V) -> X): List<X> = _features.values.map(action)
}
