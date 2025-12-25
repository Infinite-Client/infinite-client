package org.infinite.libs.core.features

import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.utils.toLowerSnakeCase
import java.util.concurrent.ConcurrentHashMap

open class Feature : MinecraftInterface() {
    private val properties: ConcurrentHashMap<String, Property<*>> = ConcurrentHashMap()

    /**
     * 指定された名前の翻訳キーを取得します。
     * @param name プロパティ名。null の場合は Feature 自身のキーを返します。
     * @return 翻訳キー。プロパティ名が指定され、かつ存在しない場合は null を返します。
     */
    fun translation(name: String? = null): String? {
        if (name == null) return translationKey

        // 設定値（Property）が存在するか確認
        return if (properties.containsKey(name)) {
            "$translationKey.${name.toLowerSnakeCase()}"
        } else {
            null
        }
    }

    fun data(): Map<String, Any?> {
        val data = mutableMapOf<String, Any?>()
        properties.forEach { (name, property) ->
            data[name.toLowerSnakeCase()] = property.value
        }
        return data
    }

    /**
     * Feature自身と、保有する全てのプロパティの翻訳キーをリストで取得します。
     */
    val translations: List<String>
        get() {
            val list = mutableListOf(translationKey)
            list.addAll(properties.keys.map { "$translationKey.${it.toLowerSnakeCase()}" })
            return list
        }

    fun list(): List<Pair<String, Property<*>>> = properties.toList()

    protected fun <T, P : Property<T>> property(name: String, property: P): P {
        properties[name] = property
        return property
    }

    fun <T> get(name: String): T? {
        @Suppress("UNCHECKED_CAST")
        return properties[name]?.value as? T
    }

    fun <T> set(name: String, value: T) {
        val prop = properties[name] as? Property<T>
        prop?.value = value
    }

    private val translationKey: String by lazy {
        val modId = "ultimate"
        val translationCategory = "features"

        val fullName = this::class.qualifiedName
            ?: throw IllegalArgumentException("Qualified name not found for ${this::class.simpleName}")

        val parts = fullName.split(".")
        val size = parts.size

        if (size >= 4) {
            val className = parts.last().toLowerSnakeCase()
            val category = parts[size - 3].toLowerSnakeCase()
            val scope = parts[size - 4].toLowerSnakeCase()

            "$modId.$translationCategory.$scope.$category.$className"
        } else {
            throw IllegalArgumentException("Package hierarchy is too shallow: $fullName")
        }
    }
}
