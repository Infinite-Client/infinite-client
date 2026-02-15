package org.infinite.libs.config

import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.log.LogSystem
import org.infinite.utils.toLowerSnakeCase
import java.io.File
import kotlin.reflect.full.createType

object ConfigManager : MinecraftInterface() {
    private val baseDir = File(minecraft.run { gameDirectory }, "infinite/config")

    private val json: Json by lazy {
        Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
            ignoreUnknownKeys = true
            // クラスの継承などがある場合に備えて
            allowStructuredMapKeys = true
        }
    }

    // --- Save ---

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

            // data (Map<CategoryName, Map<FeatureName, FeatureData>>) を JsonElement に変換
            val jsonElement = encodeToElement(data)
            val jsonString = json.encodeToString(JsonElement.serializer(), jsonElement)

            file.writeText(jsonString)
        } catch (e: Exception) {
            LogSystem.error("Failed to save config: ${e.stackTraceToString()}")
        }
    }

    /**
     * @Serializable がついたオブジェクトを動的に JsonElement に変換する
     */
    private fun encodeToElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull

        is JsonElement -> value

        // --- プリミティブ型を encodeToJsonElement に通さず直接扱う ---
        is String -> JsonPrimitive(value)

        is Number -> JsonPrimitive(value)

        is Boolean -> JsonPrimitive(value)

        is Feature.FeatureData -> buildJsonObject {
            put("enabled", JsonPrimitive(value.enabled))
            put("properties", encodeToElement(value.properties))
        }

        is Map<*, *> -> buildJsonObject {
            value.forEach { (k, v) -> put(k.toString(), encodeToElement(v)) }
        }

        is Iterable<*> -> buildJsonArray {
            value.forEach { add(encodeToElement(it)) }
        }

        // カスタムクラス (BlockAndColor等) のみシリアライザーを使用
        else -> try {
            json.encodeToJsonElement(json.serializersModule.serializer(value::class.createType()), value)
        } catch (e: Exception) {
            LogSystem.error("Failed to encode custom class ${value::class.simpleName}: ${e.message}")
            JsonPrimitive(value.toString())
        }
    }
    // --- Load ---

    fun loadGlobal() {
        ensureGlobal()
        val file = File(baseDir, "global.json")
        if (file.exists()) {
            val jsonElement = loadJsonElement(file)
            if (jsonElement is JsonObject) applyData(InfiniteClient.globalFeatures, jsonElement)
        }
        saveGlobal()
    }

    fun loadLocal() {
        InfiniteClient.localFeatures.reset()
        ensureLocal()
        getLocalPath()?.let { path ->
            val file = File(baseDir, "local/$path/local.json")
            if (file.exists()) {
                val jsonElement = loadJsonElement(file)
                if (jsonElement is JsonObject) applyData(InfiniteClient.localFeatures, jsonElement)
            }
        }
        saveLocal()
    }

    private fun loadJsonElement(file: File): JsonElement = try {
        json.parseToJsonElement(file.readText())
    } catch (e: Exception) {
        LogSystem.error("Failed to parse config JSON: ${e.message}")
        JsonObject(emptyMap())
    }

    private fun applyData(
        categoriesObj: org.infinite.libs.core.features.FeatureCategories<*, *, *, *>,
        data: JsonObject,
    ) {
        data.forEach { (categoryName, featuresElement) ->
            if (featuresElement !is JsonObject) return@forEach

            val category = categoriesObj.categories.values.find { cat ->
                val id = cat::class.qualifiedName?.split(".")?.let {
                    if (it.size >= 2) it[it.size - 2].toLowerSnakeCase() else null
                }
                id == categoryName
            } ?: run {
                LogSystem.warn("Category not found: $categoryName")
                return@forEach
            }

            featuresElement.forEach { (featureName, featureDataElement) ->
                if (featureDataElement !is JsonObject) return@forEach

                val feature = category.features.values.find { feat ->
                    feat::class.simpleName?.toLowerSnakeCase() == featureName
                } ?: return@forEach

                // --- Enabled ---
                featureDataElement["enabled"]?.jsonPrimitive?.booleanOrNull?.let {
                    if (it) feature.enable() else feature.disable()
                }

                // --- Properties ---
                val props = featureDataElement["properties"]?.jsonObject ?: return@forEach
                props.forEach { (propName, jsonValue) ->
                    applyPropertySafely(feature, propName, jsonValue)
                }
            }
        }
    }

    private fun applyPropertySafely(feature: Feature, propName: String, value: JsonElement) {
        feature.tryApply(propName, value)
    }

    // --- Utilities ---
    private fun ensureGlobal() {
        InfiniteClient.globalFeatures.categories.values.forEach { it.features.values.forEach { f -> f.ensureAllPropertiesRegistered() } }
    }

    private fun ensureLocal() {
        InfiniteClient.localFeatures.categories.values.forEach { it.features.values.forEach { f -> f.ensureAllPropertiesRegistered() } }
    }

    private var lastLocalPath: String? = null

    private fun getLocalPath(): String? {
        val isLocal = minecraft.isLocalServer
        val name = if (isLocal) minecraft.singleplayerServer?.storageSource?.levelId else minecraft.currentServer?.name
        val path = name?.let { "${if (isLocal) "sp" else "mp"}/$it" }
        if (path != null) lastLocalPath = path
        return path ?: lastLocalPath
    }
}
