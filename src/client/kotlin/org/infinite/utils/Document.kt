package org.infinite.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Property
import org.infinite.libs.core.features.property.*
import org.infinite.libs.core.features.property.list.*
import org.infinite.libs.core.features.property.number.*
import org.infinite.libs.core.features.property.selection.*
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.*

class Document(
    val projectDir: Path,
) {
    data class AllFeaturesDocumentData(
        val local: List<CategoryDocumentData>,
        val global: List<CategoryDocumentData>,
    )

    data class CategoryDocumentData(
        val name: String,
        val features: List<FeatureDocumentData>,
    )

    data class FeatureDocumentData(
        val name: String,
        val key: String,
        val settings: List<SettingDocumentData>,
    )

    data class SettingDocumentData(
        val name: String,
        val key: String,
        val setting: Property<*>,
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Documentクラスのmain関数が実行されました。")
            // Minecraftの初期化が必要な場合があるため、ブートストラップを呼び出す
            try {
                val scClass = Class.forName("net.minecraft.SharedConstants")
                try {
                    scClass.getMethod("tryCheckInitialize").invoke(null)
                } catch (e: NoSuchMethodException) {
                    try {
                        scClass.getMethod("create").invoke(null)
                    } catch (e2: NoSuchMethodException) {
                        // Ignore
                    }
                }
                net.minecraft.server.Bootstrap.bootStrap()
            } catch (e: Throwable) {
                // Ignore
            }

            val rootPath = args[0]
            val document = Document(Path(rootPath))
            println("Documentのインスタンスを作成しました: $document")
            document.generateData()
            document.getTranslations()
            document.generateDocs()
        }
    }

    private val buildDir: Path
        get() = projectDir.resolve("build")
    private var documentData: AllFeaturesDocumentData? = null
    private var translations: Map<String, Map<String, String>> = emptyMap()

    @OptIn(ExperimentalPathApi::class)
    fun generateDocs() {
        val data = documentData ?: return
        val supportedLocales = translations.keys

        if (supportedLocales.isEmpty()) {
            System.err.println("警告: 翻訳ファイルが見つからなかったため、ドキュメント生成をスキップします。")
            return
        }

        val docsBaseDir = buildDir.resolve("docs")

        if (docsBaseDir.isDirectory()) {
            docsBaseDir.deleteRecursively()
            println("既存のDocsディレクトリをクリアしました: $docsBaseDir")
        }

        initializeDocsDirectory(docsBaseDir, supportedLocales)

        supportedLocales.forEach { langCode ->
            val langDir = docsBaseDir.resolve(langCode)
            generateFeaturesListDocs(
                dir = langDir.resolve("local-features"),
                data = data.local,
                langCode = langCode,
            )
            generateFeaturesListDocs(langDir.resolve("global-features"), data.global, langCode)
            println("✅ $langCode のドキュメントを $langDir に生成しました。")
        }
    }

    private fun generateFeaturesListDocs(
        dir: Path,
        data: List<CategoryDocumentData>,
        langCode: String,
    ) {
        val isLocal = dir.fileName.toString() == "local-features"
        val label =
            if (isLocal) {
                if (langCode == "ja_jp") "ローカル機能" else "Local Features"
            } else {
                if (langCode == "ja_jp") "グローバル機能" else "Global Features"
            }

        dir.resolve("_category_.json").writeText(
            """
            {
              "label": "$label",
              "position": ${if (isLocal) 1 else 2},
              "link": {
                "type": "generated-index",
                "title": "$label"
              }
            }
            """.trimIndent(),
            StandardCharsets.UTF_8,
        )

        data.forEach { category ->
            generateCategoryDocs(
                parentDir = dir,
                category = category,
                langCode = langCode,
            )
        }
    }

    private fun generateCategoryDocs(
        parentDir: Path,
        category: CategoryDocumentData,
        langCode: String,
    ) {
        val categoryFolderName = category.name.toKebabCase()
        val categoryDir = parentDir.resolve(categoryFolderName)
        categoryDir.createDirectories()

        val label = translate(category.name, langCode)
        val isLocal = parentDir.fileName?.toString() == "local-features"
        categoryDir.resolve("_category_.json").writeText(
            """
            {
              "label": "$label(${if (isLocal) "Local" else "Global"})",
              "link": {
                "type": "generated-index", 
                "title": "$label 機能一覧"
              }
            }
            """.trimIndent(),
            StandardCharsets.UTF_8,
        )
        println("  - カテゴリメタファイル生成: ${categoryDir.resolve("_category_.json")}")

        category.features.forEach { feature ->
            generateFeatureDocs(
                dir = categoryDir,
                data = feature,
                langCode = langCode,
            )
        }
    }

    private fun generateFeatureDocs(
        dir: Path,
        data: FeatureDocumentData,
        langCode: String,
    ) {
        val featureFileName = data.name.toKebabCase() + ".mdx"
        val featurePath = dir.resolve(featureFileName)

        val featureContent = StringBuilder()
        featureContent.append("---\n")
        featureContent.append("title: ${data.name.escapeMdx()}\n")
        featureContent.append("---\n\n")

        featureContent.append("# ${data.name.escapeMdx()}\n")
        featureContent.append("${translate(data.key, langCode).escapeMdx()}\n\n")

        if (data.settings.isNotEmpty()) {
            featureContent.append("## ${translate("doc.infinite.properties_section_title", langCode).escapeMdx()}\n\n")

            data.settings.forEach { settingData ->
                val setting = settingData.setting
                featureContent.append("### ${settingData.name.escapeMdx()}\n")
                featureContent.append("${translate(settingData.key, langCode).escapeMdx()}\n\n")
                featureContent.append("#### ${translate("doc.infinite.property_info_title", langCode).escapeMdx()}\n\n")

                val shortTypeName = setting::class.simpleName?.removeSuffix("Property") ?: "Unknown"

                featureContent.append("* **${translate("doc.infinite.property_type", langCode).escapeMdx()}**: `$shortTypeName`\n")
                featureContent.append("* **${translate("doc.infinite.property_default", langCode).escapeMdx()}**: `${setting.default}`\n")

                when (setting) {
                    is IntProperty -> {
                        featureContent.append("* **${translate("doc.infinite.property_min", langCode).escapeMdx()}**: `${setting.min}`\n")
                        featureContent.append("* **${translate("doc.infinite.property_max", langCode).escapeMdx()}**: `${setting.max}`\n")
                    }

                    is FloatProperty -> {
                        featureContent.append("* **${translate("doc.infinite.property_min", langCode).escapeMdx()}**: `${setting.min}`\n")
                        featureContent.append("* **${translate("doc.infinite.property_max", langCode).escapeMdx()}**: `${setting.max}`\n")
                    }

                    is DoubleProperty -> {
                        featureContent.append("* **${translate("doc.infinite.property_min", langCode).escapeMdx()}**: `${setting.min}`\n")
                        featureContent.append("* **${translate("doc.infinite.property_max", langCode).escapeMdx()}**: `${setting.max}`\n")
                    }

                    is LongProperty -> {
                        featureContent.append("* **${translate("doc.infinite.property_min", langCode).escapeMdx()}**: `${setting.min}`\n")
                        featureContent.append("* **${translate("doc.infinite.property_max", langCode).escapeMdx()}**: `${setting.max}`\n")
                    }

                    is StringListProperty -> {
                        featureContent.append("* **${translate("doc.infinite.property_options", langCode).escapeMdx()}**: ${setting.default.joinToString(", ") { "`$it`" }}\n")
                    }

                    is SelectionProperty<*> -> {
                        featureContent.append("* **${translate("doc.infinite.property_options", langCode).escapeMdx()}**: ${setting.options.joinToString(", ") { "`$it`" }}\n")
                    }

                    is BlockListProperty -> {
                        featureContent.append("* **${translate("doc.infinite.property_list_count", langCode).escapeMdx()}**: ${setting.default.size}\n")
                        featureContent.append("* **${translate("doc.infinite.property_list_type", langCode).escapeMdx()}**: Block IDs\n")
                    }

                    is ItemListProperty -> {
                        featureContent.append("* **${translate("doc.infinite.property_list_count", langCode).escapeMdx()}**: ${setting.default.size}\n")
                        featureContent.append("* **${translate("doc.infinite.property_list_type", langCode).escapeMdx()}**: Item IDs\n")
                    }

                    is BlockAndColorListProperty -> {
                        featureContent.append("* **${translate("doc.infinite.property_list_count", langCode).escapeMdx()}**: ${setting.default.size}\n")
                        featureContent.append("* **${translate("doc.infinite.property_list_type", langCode).escapeMdx()}**: Block IDs to Color Map\n")
                    }
                }
                featureContent.append("\n---\n\n")
            }
        }
        featurePath.writeText(featureContent.toString(), StandardCharsets.UTF_8)
        println("  - ファイル生成: $featurePath")
    }

    private fun String.escapeMdx(): String = this.replace("<", "&lt;").replace(">", "&gt;")

    private fun translate(key: String, lang: String): String = translations[lang]?.get(key) ?: key

    private fun initializeDocsDirectory(
        docsBaseDir: Path,
        supportedLocales: Set<String>,
    ) {
        docsBaseDir.createDirectories()
        println("作成されたDocsディレクトリ: $docsBaseDir")

        val data = documentData ?: return

        supportedLocales.forEach { langCode ->
            val langDir = docsBaseDir.resolve(langCode)
            val localFeaturesDir = langDir.resolve("local-features")
            localFeaturesDir.createDirectories()

            data.local.forEach { category ->
                val categoryDir = localFeaturesDir.resolve(category.name.toKebabCase())
                categoryDir.createDirectories()
            }

            val globalFeaturesDir = langDir.resolve("global-features")
            globalFeaturesDir.createDirectories()

            data.global.forEach { category ->
                val categoryDir = globalFeaturesDir.resolve(category.name.toKebabCase())
                categoryDir.createDirectories()
            }
        }
    }

    fun getTranslations() {
        var langDirPath = projectDir.resolve("infinite-client/src/main/resources/assets/infinite-client/lang/")
        if (!langDirPath.isDirectory()) {
            langDirPath = projectDir.resolve("src/main/resources/assets/infinite-client/lang/")
        }
        if (!langDirPath.isDirectory()) {
            System.err.println("エラー: 翻訳ディレクトリが見つからないか、ディレクトリではありません: $langDirPath")
            return
        }

        val allTranslations = mutableMapOf<String, Map<String, String>>()
        val gson = Gson()
        val translationFiles = langDirPath.listDirectoryEntries("*.json")

        translationFiles.forEach { langFile ->
            try {
                val langCode = langFile.nameWithoutExtension
                val jsonString = langFile.readText(StandardCharsets.UTF_8)
                val mapType = object : TypeToken<Map<String, String>>() {}.type
                val translationsMap = gson.fromJson<Map<String, String>>(jsonString, mapType)
                allTranslations[langCode] = translationsMap
            } catch (e: Exception) {
                System.err.println("エラー: 翻訳ファイル ${langFile.fileName} の読み込みまたはパースに失敗しました: ${e.message}")
            }
        }
        translations = allTranslations
    }

    fun generateData() {
        println("localFeatures: ${InfiniteClient.featureCategories.size}")
        println("globalFeatures: ${InfiniteClient.globalFeatureCategories.size}")
        InfiniteClient.genTranslations(projectDir.absolutePathString())

        val localDocumentData = InfiniteClient.featureCategories.map { category ->
            val name = category.name
            val features = category.map { feature ->
                val featureName = feature.name
                val featureKey = feature.translation()
                val settings = feature.map { property ->
                    val settingName = property.name
                    val settingKey = property.translationKey() ?: ""
                    SettingDocumentData(settingName, settingKey, property)
                }
                FeatureDocumentData(featureName, featureKey, settings)
            }
            CategoryDocumentData(name, features)
        }

        val globalDocumentData = InfiniteClient.globalFeatureCategories.map { category ->
            val name = category.name
            val features = category.map { feature ->
                val featureName = feature.name
                val featureKey = feature.translation()
                val settings = feature.map { property ->
                    val settingName = property.name
                    val settingKey = property.translationKey() ?: ""
                    SettingDocumentData(settingName, settingKey, property)
                }
                FeatureDocumentData(featureName, featureKey, settings)
            }
            CategoryDocumentData(name, features)
        }
        documentData = AllFeaturesDocumentData(localDocumentData, globalDocumentData)
    }
}
