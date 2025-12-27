package org.infinite.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.infinite.InfiniteClient
import org.infinite.settings.FeatureSetting
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.collections.map
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText

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
        val setting: FeatureSetting<*>,
    )

    // クラス名と関連付けて、静的なメンバ（Javaのstaticに相当）を保持するオブジェクト
    companion object {
        @JvmStatic // JVMから静的メソッドとして呼び出せるようにする（必須ではないが推奨）
        fun main(args: Array<String>) {
            // ここにプログラムの実行時に行いたい処理を記述します。
            println("Documentクラスのmain関数が実行されました。")
            val rootPath = args[0]
            // 例: Documentクラスのインスタンスを作成して利用
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

    /**
     * データを元にドキュメントを生成し、ディレクトリ構造を初期化します。
     */
    @OptIn(ExperimentalPathApi::class)
    fun generateDocs() {
        val data = documentData ?: return
        // 翻訳データのキーセット（言語コード）を動的に取得
        val supportedLocales = translations.keys

        if (supportedLocales.isEmpty()) {
            System.err.println("警告: 翻訳ファイルが見つからなかったため、ドキュメント生成をスキップします。")
            return
        }

        val docsBaseDir = buildDir.resolve("docs")

        // 1. docs ディレクトリのクリア
        if (docsBaseDir.isDirectory()) {
            docsBaseDir.deleteRecursively()
            println("既存のDocsディレクトリをクリアしました: $docsBaseDir")
        }

        // 2. docs ディレクトリ構造の初期化
        initializeDocsDirectory(docsBaseDir, supportedLocales) // 言語リストを渡す

        // 3. 言語ごとにMDXファイルを生成
        supportedLocales.forEach { langCode ->
            val langDir = docsBaseDir.resolve(langCode)

            // ローカル機能のドキュメントを生成
            generateFeaturesListDocs(
                dir = langDir.resolve("local-features"), // スネークケース
                data = data.local,
                langCode = langCode,
            )
            generateFeaturesListDocs(langDir.resolve("global-features"), data.global, langCode) // スネークケース

            println("✅ $langCode のドキュメントを $langDir に生成しました。")
        }
    }

    private fun generateFeaturesListDocs(
        dir: Path,
        data: List<CategoryDocumentData>,
        langCode: String,
    ) {
        // 1. ベースディレクトリの _category_.json を生成
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

        // 2. 各カテゴリの処理を委譲
        data.forEach { category ->
            generateCategoryDocs(
                parentDir = dir, // 親ディレクトリ (local-features/ または global-features/) を渡す
                category = category,
                langCode = langCode,
            )
        }
    }

    /**
     * 特定のカテゴリのインデックスファイル (_category_index.mdx) を生成し、
     * 個別の機能の生成を generateFeatureDocs に委譲します。
     */
    private fun generateCategoryDocs(
        parentDir: Path,
        category: CategoryDocumentData,
        langCode: String,
    ) {
        // カテゴリ名で新しいディレクトリを解決 (スネークケース)
        val categoryFolderName = toKebabCase(category.name)
        val categoryDir = parentDir.resolve(categoryFolderName)

        // 1. Docusaurus用メタファイル (_category_.json) を生成
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

        // 2. 個別の機能ドキュメントを生成
        category.features.forEach { feature ->
            generateFeatureDocs(
                dir = categoryDir, // 新しいカテゴリディレクトリを渡す
                data = feature,
                langCode = langCode,
            )
        }
    }

    /**
     * 個別の機能とその設定に関するMDXファイルを生成します。
     * @param dir 機能ファイルを出力するディレクトリ
     * @param data 機能データ
     * @param langCode 言語コード (例: "ja_jp")
     */
    private fun generateFeatureDocs(
        dir: Path,
        data: FeatureDocumentData,
        langCode: String,
    ) {
        // ファイル名を決定
        val featureFileName = toKebabCase(data.name) + ".mdx"
        val featurePath = dir.resolve(featureFileName)

        val featureContent = StringBuilder()

        // --- MDXファイルのフロントマター (Docusaurus用) ---
        featureContent.append("---\n")
        featureContent.append("title: ${data.name}\n")
        featureContent.append("---\n\n")

        // --- 機能のタイトルと概要 (修正された形式) ---
        featureContent.append("# ${data.name}\n") // H1見出しはデータ名
        // 機能の説明（翻訳）
        featureContent.append("${translate(data.key, langCode)}\n\n")

        // --- 設定セクション ---
        if (data.settings.isNotEmpty()) {
            featureContent.append("## ${translate("doc.infinite.settings_section_title", langCode)}\n\n")

            data.settings.forEach { settingData ->
                val setting = settingData.setting

                // 設定名 (H3見出し) は name を使用し、コード部分は削除
                featureContent.append("### ${settingData.name}\n")

                // 説明 (翻訳された設定キーの内容)
                featureContent.append("${translate(settingData.key, langCode)}\n\n")

                // --- 設定情報 ---
                featureContent.append("#### ${translate("doc.infinite.setting_info_title", langCode)}\n\n")

                // タイプ名の短縮
                val shortTypeName = setting::class.simpleName?.removeSuffix("Setting") ?: "Unknown"

                featureContent.append(
                    "* **${
                        translate(
                            "doc.infinite.setting_type",
                            langCode,
                        )
                    }**: `$shortTypeName`\n",
                )
                featureContent.append(
                    "* **${
                        translate(
                            "doc.infinite.setting_default",
                            langCode,
                        )
                    }**: `${setting.defaultValue}`\n",
                )

                when (setting) {
                    // 範囲設定 (Int, Float, Double)
                    is FeatureSetting.IntSetting -> {
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_min",
                                    langCode,
                                )
                            }**: `${setting.min}`\n",
                        )
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_max",
                                    langCode,
                                )
                            }**: `${setting.max}`\n",
                        )
                    }

                    is FeatureSetting.FloatSetting -> {
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_min",
                                    langCode,
                                )
                            }**: `${setting.min}`\n",
                        )
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_max",
                                    langCode,
                                )
                            }**: `${setting.max}`\n",
                        )
                    }

                    is FeatureSetting.DoubleSetting -> {
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_min",
                                    langCode,
                                )
                            }**: `${setting.min}`\n",
                        )
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_max",
                                    langCode,
                                )
                            }**: `${setting.max}`\n",
                        )
                    }

                    // 選択肢設定 (StringList, Enum)
                    is FeatureSetting.StringListSetting -> {
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_options",
                                    langCode,
                                )
                            }**: ${setting.options.joinToString(", ") { "`$it`" }}\n",
                        )
                    }

                    is FeatureSetting.EnumSetting<*> -> {
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_options",
                                    langCode,
                                )
                            }**: ${setting.options.joinToString(", ") { "`$it`" }}\n",
                        )
                    }

                    // リスト/マップ設定 (リストの要素数を出力)
                    is FeatureSetting.BlockListSetting -> {
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_list_count",
                                    langCode,
                                )
                            }**: ${setting.defaultValue.size}\n",
                        )
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_list_type",
                                    langCode,
                                )
                            }**: Block IDs\n",
                        )
                    }

                    is FeatureSetting.EntityListSetting -> {
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_list_count",
                                    langCode,
                                )
                            }**: ${setting.defaultValue.size}\n",
                        )
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_list_type",
                                    langCode,
                                )
                            }**: Entity IDs\n",
                        )
                    }

                    is FeatureSetting.PlayerListSetting -> {
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_list_count",
                                    langCode,
                                )
                            }**: ${setting.defaultValue.size}\n",
                        )
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_list_type",
                                    langCode,
                                )
                            }**: Player Names\n",
                        )
                    }

                    is FeatureSetting.BlockColorListSetting -> {
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_list_count",
                                    langCode,
                                )
                            }**: ${setting.defaultValue.size}\n",
                        )
                        featureContent.append(
                            "* **${
                                translate(
                                    "doc.infinite.setting_list_type",
                                    langCode,
                                )
                            }**: Block IDs to Color Map\n",
                        )
                    }

                    // ID設定 (特に固有情報なし。タイプ情報が十分)
                    is FeatureSetting.BlockIDSetting -> {
                        // 追加情報なし
                    }

                    is FeatureSetting.EntityIDSetting -> {
                        // 追加情報なし
                    }

                    // シンプルな設定 (Boolean, String)
                    is FeatureSetting.BooleanSetting -> {
                        // 追加情報なし
                    }

                    is FeatureSetting.StringSetting -> {
                        // 追加情報なし
                    }
                }
                featureContent.append("\n---\n\n") // 設定間の区切り
            }
        }

        // MDXファイルへの書き込み
        featurePath.writeText(featureContent.toString(), StandardCharsets.UTF_8)
        println("  - ファイル生成: $featurePath")
    }

    private fun translate(
        key: String,
        lang: String,
    ): String {
        return translations[lang]?.get(key) ?: key // 翻訳が見つからない場合はキーそのものを返す
    }

    // ... (initializeDocsDirectory, getTranslations, generateData, toKebabCase 関数は省略) ...

    /**
     * docs/、docs/ja_jp/、docs/en_us/ などのディレクトリ構造を作成します。
     * @param supportedLocales 翻訳ファイルから取得した言語コードのセット
     */
    private fun initializeDocsDirectory(
        docsBaseDir: Path,
        supportedLocales: Set<String>,
    ) {
        // ベースディレクトリを作成
        docsBaseDir.createDirectories()
        println("作成されたDocsディレクトリ: $docsBaseDir")

        val data = documentData ?: return // documentDataがnullならここで抜ける

        supportedLocales.forEach { langCode ->
            val localFeaturesDir = docsBaseDir.resolve(langCode).resolve("local-features") // スネークケース

            // local-features のベースディレクトリを作成
            localFeaturesDir.createDirectories()
            println("作成されたDocsディレクトリ: $localFeaturesDir")

            // カテゴリごとのサブディレクトリを作成
            data.local.forEach { category ->
                val categoryDir = localFeaturesDir.resolve(toKebabCase(category.name))
                categoryDir.createDirectories()
                println("  - カテゴリディレクトリ作成: $categoryDir")
            }

            // グローバル機能についても同様にカテゴリディレクトリを作成
            val globalFeaturesDir = docsBaseDir.resolve(langCode).resolve("global-features") // スネークケース
            globalFeaturesDir.createDirectories()

            data.global.forEach { category ->
                val categoryDir = globalFeaturesDir.resolve(toKebabCase(category.name))
                categoryDir.createDirectories()
                println("  - カテゴリディレクトリ作成: $categoryDir")
            }
        }
    }

    fun getTranslations() {
        val langDirPath = projectDir.resolve("src/main/resources/assets/infinite/lang/")
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
        println(allTranslations)
        translations = allTranslations
    }

    fun generateData() {
        // ... (generateData のロジックは変更なし) ...
        println("localFeatures: ${InfiniteClient.featureCategories.size}")
        println("globalFeatures: ${InfiniteClient.globalFeatureCategories.size}")
        InfiniteClient.genTranslations()
        val localDocumentData =
            InfiniteClient.featureCategories.map { category ->
                val name = category.name
                val features =
                    category.features.map { feature ->
                        val name = feature.name
                        val key = feature.descriptionKey
                        val settings =
                            feature.instance.settings.map {
                                val name = it.name
                                val key = it.descriptionKey
                                SettingDocumentData(name, key, it)
                            }
                        FeatureDocumentData(name, key, settings)
                    }
                CategoryDocumentData(name, features)
            }
        val globalDocumentData =
            InfiniteClient.globalFeatureCategories.map { category ->
                val name = category.name
                val features =
                    category.features.map { feature ->
                        val name = feature.name
                        val key = feature.descriptionKey
                        val settings =
                            feature.instance.settings.map {
                                val name = it.name
                                val key = it.descriptionKey
                                SettingDocumentData(name, key, it)
                            }
                        FeatureDocumentData(name, key, settings)
                    }
                CategoryDocumentData(name, features)
            }
        documentData = AllFeaturesDocumentData(localDocumentData, globalDocumentData)
    }
}
