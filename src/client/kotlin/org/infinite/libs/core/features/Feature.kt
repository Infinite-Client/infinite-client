package org.infinite.libs.core.features

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.infinite.InfiniteClient
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.log.LogSystem
import org.infinite.utils.toLowerSnakeCase
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

open class Feature : MinecraftInterface() {
    // --- 追加: カテゴリ参照 ---
    /**
     * このFeatureが属するカテゴリのクラス。依存解決に使用します。
     * 各Featureの実装クラスで override して指定してください。
     */
    open val categoryClass: KClass<out Category<*, *>>? = null

    // --- 1. 定義とステータス ---
    enum class FeatureType { Utils, Extend, Cheat }

    private val _properties: MutableMap<String, Property<*>> =
        Collections.synchronizedMap(LinkedHashMap<String, Property<*>>())

    // 外部公開用（順序が維持された Map が返る）
    val properties: Map<String, Property<*>> get() = _properties

    private fun register(name: String, property: Property<*>) {
        // LinkedHashMap なので、最初に追加された順序が守られる
        if (!_properties.containsKey(name)) {
            _properties[name] = property
        }
    }

    val enabled = Property(false)
    open val name: String = this::class.simpleName ?: "UnknownFeature"
    open val featureType: FeatureType = FeatureType.Utils

    // --- 2. 依存・矛盾関係の管理 ---
    // ここも KClass を保持するように定義を明確化
    open val depends: List<KClass<out Feature>> = emptyList()
    open val conflicts: List<KClass<out Feature>> = emptyList()
    private val listenerLock = Any()
    private val dependencyListeners = CopyOnWriteArrayList<() -> Unit>()

    init {
        enabled.addListener { _, isEnabled ->
            if (isEnabled) {
                resolveDependencies()
                onEnabled()
            } else {
                onDisabled()
            }
        }
    }

    // --- 3. プロパティ委譲ロジック ---
    protected fun <T, P : Property<T>> property(property: P): PropertyDelegate<T, P> = PropertyDelegate(property)

    protected inner class PropertyDelegate<T, P : Property<T>>(val property: P) {
        operator fun getValue(thisRef: Feature, prop: KProperty<*>): P {
            register(prop.name, property)
            return property
        }
    }

    private fun ensureAllPropertiesRegistered() {
        this::class.declaredMemberProperties.forEach { prop ->
            try {
                prop.isAccessible = true
                prop.getter.call(this)
            } catch (e: Exception) {
                LogSystem.error("Failed to register property ${prop.name}: $e")
            }
        }
    }

    open fun onEnabled() {}
    open fun onDisabled() {}
    fun enable() {
        if (isEnabled()) return
        startResolver()
        enabled.value = true
    }

    fun disable() {
        if (!isEnabled()) return
        stopResolver()
        enabled.value = false
    }

    fun isEnabled(): Boolean = enabled.value
    fun toggle() = if (isEnabled()) disable() else enable()
    fun reset() {
        _properties.forEach { prop ->
            prop.value.reset()
        }
        disable()
    }

    private fun startResolver() = synchronized(listenerLock) {
        val cat = categoryClass ?: return@synchronized

        depends.forEach { target ->
            val feat = InfiniteClient.feature(cat, target) ?: return@forEach
            val listener: (Boolean, Boolean) -> Unit = { _, newVal -> if (!newVal) disable() }
            feat.enabled.addListener(listener)
            dependencyListeners.add { feat.enabled.removeListener(listener) }
        }
        conflicts.forEach { target ->
            val feat = InfiniteClient.feature(cat, target) ?: return@forEach
            val listener: (Boolean, Boolean) -> Unit = { _, newVal -> if (newVal) disable() }
            feat.enabled.addListener(listener)
            dependencyListeners.add { feat.enabled.removeListener(listener) }
        }
    }

    private fun stopResolver() = synchronized(listenerLock) {
        dependencyListeners.forEach { it() }
        dependencyListeners.clear()
    }

    private fun resolveDependencies() {
        val cat = categoryClass ?: return

        depends.forEach { InfiniteClient.feature(cat, it)?.let { f -> if (!f.isEnabled()) f.enable() } }
        conflicts.forEach { InfiniteClient.feature(cat, it)?.let { f -> if (f.isEnabled()) f.disable() } }
    }

    // --- 6. データ管理・翻訳 (変更なし) ---
    @Serializable
    data class FeatureData(val enabled: Boolean, val properties: Map<String, @Contextual Any?>)

    fun data(): FeatureData {
        ensureAllPropertiesRegistered()
        return FeatureData(
            enabled = isEnabled(),
            properties = _properties.mapKeys { it.key.toLowerSnakeCase() }.mapValues { it.value.value },
        )
    }

    fun translation(): String = translationKey
    fun translation(p: String): String? {
        return if (_properties[p] == null) {
            null
        } else {
            translationKey + "." + p.toLowerSnakeCase()
        }
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
        val fullName = this::class.qualifiedName ?: "unknown"
        val parts = fullName.split(".")
        if (parts.size >= 4) {
            val className = parts.last().toLowerSnakeCase()
            val category = parts[parts.size - 3].toLowerSnakeCase()
            val scope = parts[parts.size - 4].toLowerSnakeCase()
            "infinite.features.$scope.$category.$className"
        } else {
            "infinite.features.general.${parts.last().toLowerSnakeCase()}"
        }
    }
}
