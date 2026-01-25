package org.infinite.libs.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.log.LogSystem
import org.infinite.utils.toLowerSnakeCase
import java.io.File

object ConfigManager : MinecraftInterface() {
    private val baseDir = File(minecraft.run { gameDirectory }, "infinite/config")

    private val json: Json by lazy {
        Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    /**
     * Map<String, Any?> を JsonElement と相互変換するシリアライザー
     */
    object GenericMapSerializer : KSerializer<Map<String, Any?>> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GenericMap")

        override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
            val jsonObject = JsonObject(value.mapValues { it.value.toJsonElement() })
            JsonObject.serializer().serialize(encoder, jsonObject)
        }

        override fun deserialize(decoder: Decoder): Map<String, Any?> {
            val root = (decoder as? JsonDecoder)?.decodeJsonElement()
            return if (root is JsonObject) root.toMap() else throw SerializationException("Nota JsonObject")
        }

        private fun Any?.toJsonElement(): JsonElement = when (this) {
            null -> JsonNull
            is String -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is Map<*, *> -> JsonObject(this.entries.associate { it.key.toString() to it.value.toJsonElement() })
            is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
            else -> JsonPrimitive(this.toString()) // 最終手段
        }

        private fun JsonObject.toMap(): Map<String, Any?> = entries.associate { it.key to it.value.toAnyValue() }

        private fun JsonElement.toAnyValue(): Any? = when (this) {
            is JsonNull -> null
            is JsonPrimitive -> if (isString) content else (booleanOrNull ?: longOrNull ?: doubleOrNull ?: content)
            is JsonObject -> toMap()
            is JsonArray -> map { it.toAnyValue() }
        }
    }

    // --- Save ---
    fun ensureGlobal() {
        InfiniteClient.globalFeatures.categories.values.forEach { category ->
            category.features.values.forEach { feature ->
                feature.ensureAllPropertiesRegistered()
            }
        }
    }

    fun ensureLocal() {
        InfiniteClient.localFeatures.categories.values.forEach { category ->
            category.features.values.forEach { feature ->
                feature.ensureAllPropertiesRegistered()
            }
        }
    }

    fun saveGlobal() {
        ensureGlobal()
        val data = InfiniteClient.globalFeatures.data()
        save(File(baseDir, "global.json"), data)
    }

    fun saveLocal() {
        ensureLocal()
        val data = InfiniteClient.localFeatures.data()
        getLocalPath()?.let { path ->
            save(File(baseDir, "local/$path/local.json"), data)
        }
    }

    private fun save(file: File, data: Map<String, *>) {
        try {
            if (!file.parentFile.exists()) file.parentFile.mkdirs()
            @Suppress("UNCHECKED_CAST")
            val plainMap = deepConvert(data) as Map<String, Any?>
            val jsonString = json.encodeToString(GenericMapSerializer, plainMap)
            file.writeText(jsonString)
        } catch (e: Exception) {
            LogSystem.error("Failed to save config: ${e.message}")
        }
    }

    /**
     * あらゆる階層の FeatureData やネストした Map を、
     * シリアライザーが解釈できる Map<String, Any?> に再帰的に変換します。
     */
    private fun deepConvert(data: Any?): Any? = when (data) {
        // FeatureData クラスをプレーンな Map に変換
        is Feature.FeatureData -> mapOf(
            "enabled" to data.enabled,
            "properties" to deepConvert(data.properties),
        )

        // Map の場合は中身を再帰的に変換
        is Map<*, *> -> data.entries.associate {
            it.key.toString() to deepConvert(it.value)
        }

        // リスト等の場合は各要素を再帰的に変換
        is Iterable<*> -> data.map { deepConvert(it) }

        // それ以外（String, Number, Boolean, null）はそのまま返す
        else -> data
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull

        is String -> JsonPrimitive(this)

        is Number -> JsonPrimitive(this)

        is Boolean -> JsonPrimitive(this)

        // FeatureData が直接渡された場合のハンドリング
        is Feature.FeatureData -> JsonObject(
            mapOf(
                "enabled" to JsonPrimitive(this.enabled),
                // this.properties は Map なので、再帰呼び出しにより下の Map 分岐に入る
                "properties" to this.properties.toJsonElement(),
            ),
        )

        // Map: 各エントリーを JsonObject に変換
        is Map<*, *> -> JsonObject(
            this.entries.associate { it.key.toString() to it.value.toJsonElement() },
        )

        // Iterable: JsonArray に変換
        is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })

        // その他: 安全のために文字列として保持
        else -> JsonPrimitive(this.toString())
    }
    // --- Load ---

    fun loadGlobal() {
        ensureGlobal()
        val file = File(baseDir, "global.json")
        if (file.exists()) applyData(InfiniteClient.globalFeatures, load(file))
        saveGlobal()
    }

    fun loadLocal() {
        InfiniteClient.localFeatures.reset()
        ensureLocal()
        getLocalPath()?.let { path ->
            val file = File(baseDir, "local/$path/local.json")
            if (file.exists()) applyData(InfiniteClient.localFeatures, load(file))
        }
        saveLocal()
    }

    private fun load(file: File): Map<String, Any?> = try {
        json.decodeFromString(GenericMapSerializer, file.readText())
    } catch (e: Exception) {
        LogSystem.error("Failed to load config: ${e.message}")
        emptyMap()
    }

    /**
     * ロードしたデータを FeatureCategories 構造へ反映
     */
    private fun applyData(
        categoriesObj: org.infinite.libs.core.features.FeatureCategories<*, *, *, *>,
        data: Map<String, Any?>,
    ) {
        data.forEach { (categoryName, featuresMap) ->
            if (featuresMap !is Map<*, *>) {
                return@forEach
            }

            // 1. カテゴリの検索ログ
            val category = categoriesObj.categories.values.find { cat ->
                val id = cat::class.qualifiedName?.split(".")?.let {
                    if (it.size >= 2) it[it.size - 2].toLowerSnakeCase() else null
                }
                id == categoryName
            } ?: run {
                return@forEach
            }

            featuresMap.forEach { (featureName, featureDataRaw) ->
                val featureData = featureDataRaw as? Map<*, *> ?: return@forEach
                // 2. 機能（Feature）の検索ログ
                val feature = category.features.values.find { feat ->
                    val id = feat::class.simpleName?.toLowerSnakeCase()
                    id == featureName.toString()
                } ?: run {
                    return@forEach
                }
                // --- Enabled の適用 ---
                val isEnabled = featureData["enabled"] as? Boolean
                isEnabled?.let { if (it) feature.enable() else feature.disable() }

                // --- Properties の適用 ---
                val props = featureData["properties"] as? Map<*, *> ?: return@forEach
                props.forEach { (propName, value) ->
                    if (propName is String && value != null) {
                        applyPropertySafely(feature, propName, value)
                    }
                }
            }
        }
    }

    /**
     * プロパティの型に合わせて値を安全にキャスト・変換して適用する
     */
    private fun applyPropertySafely(feature: Feature, propName: String, value: Any) {
        feature.tryApply(propName, value)
    }

    private fun getLocalPath(): String? {
        val isLocal = minecraft.isLocalServer
        val name = if (isLocal) minecraft.singleplayerServer?.storageSource?.levelId else minecraft.currentServer?.name
        return name?.let { "${if (isLocal) "sp" else "mp"}/$it" }
    }
}
