package org.infinite.libs.core.features

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.log.LogSystem
import org.infinite.utils.toLowerSnakeCase
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

@OptIn(ExperimentalAtomicApi::class)
open class Feature : MinecraftInterface() {
    private val _properties: ConcurrentHashMap<String, Property<*>> = ConcurrentHashMap()
    private val enabled = AtomicBoolean(false)

    fun isEnabled(): Boolean = enabled.load()
    fun enable() = enabled.store(true)
    fun disable() = enabled.store(false)
    fun toggle() = if (isEnabled()) disable() else enable()

    protected fun <T, P : Property<T>> property(property: P): PropertyDelegate<T, P> {
        return PropertyDelegate(property)
    }

    protected inner class PropertyDelegate<T, P : Property<T>>(val property: P) {
        operator fun getValue(thisRef: Feature, prop: KProperty<*>): P {
            register(prop.name, property)
            return property
        }
    }

    /**
     * プロパティを明示的に登録します
     */
    private fun register(name: String, property: Property<*>) {
        if (!_properties.containsKey(name)) {
            _properties[name] = property
        }
    }

    /**
     * 未アクセスの委譲プロパティをすべてマップに登録します。
     */
    private fun ensureAllPropertiesRegistered() {
        this::class.declaredMemberProperties.forEach { prop ->
            try {
                prop.isAccessible = true
                prop.getter.call(this)
            } catch (e: Exception) {
                LogSystem.error("$e")
            }
        }
    }

    @Serializable
    data class FeatureData(
        val enabled: Boolean,
        // properties は中身が動的なので、前述の GenericMapSerializer 等で扱うか、
        // ここでは単純な構造として定義します
        val properties: Map<String, @Contextual Any?>,
    )

    fun data(): FeatureData {
        // 未アクセスの委譲プロパティをすべて登録
        ensureAllPropertiesRegistered()

        return FeatureData(
            enabled = isEnabled(),
            properties = _properties.mapKeys { (name, _) ->
                name.toLowerSnakeCase()
            }.mapValues { (_, property) ->
                property.value
            },
        )
    }

    // --- 以下、既存ロジックの調整 ---

    fun translation(name: String): String? {
        ensureAllPropertiesRegistered()
        val key = _properties.keys.find { it.equals(name, ignoreCase = true) }
        return key?.let { "$translationKey.${it.toLowerSnakeCase()}" }
    }

    fun translation(): String {
        ensureAllPropertiesRegistered()
        return translationKey
    }

    val translations: List<String>
        get() {
            ensureAllPropertiesRegistered()
            return listOf(translationKey) + _properties.keys.map { "$translationKey.${it.toLowerSnakeCase()}" }
        }

    fun list(): List<Pair<String, Property<*>>> {
        ensureAllPropertiesRegistered()
        return _properties.toList()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(name: String): T? {
        ensureAllPropertiesRegistered()
        return _properties[name]?.value as? T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> set(name: String, value: T) {
        ensureAllPropertiesRegistered()
        val prop = _properties[name] ?: _properties.entries.find { it.key.toLowerSnakeCase() == name }?.value
        (prop as? Property<T>)?.value = value
    }

    private val translationKey: String by lazy {
        val modId = "ultimate"
        val translationCategory = "features"
        val fullName = this::class.qualifiedName ?: throw IllegalArgumentException("Qualified name not found")
        val parts = fullName.split(".")
        if (parts.size >= 4) {
            val className = parts.last().toLowerSnakeCase()
            val category = parts[parts.size - 3].toLowerSnakeCase()
            val scope = parts[parts.size - 4].toLowerSnakeCase()
            "$modId.$translationCategory.$scope.$category.$className"
        } else {
            "$modId.$translationCategory.general.${parts.last().toLowerSnakeCase()}"
        }
    }
}
