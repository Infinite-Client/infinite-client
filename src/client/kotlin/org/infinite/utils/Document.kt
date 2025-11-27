package org.infinite.utils

import org.infinite.InfiniteClient
import org.infinite.settings.FeatureSetting

class Document {
    data class DocumentData(
        val en: AllFeaturesDocumentData,
        val ja: AllFeaturesDocumentData
    ) {
        companion object {
            fun empty(): DocumentData = DocumentData(
                AllFeaturesDocumentData(emptyList(), emptyList()),
                AllFeaturesDocumentData(emptyList(), emptyList())
            )
        }
    }

    data class AllFeaturesDocumentData(
        val local: List<CategoryDocumentData>,
        val global: List<CategoryDocumentData>
    )

    data class CategoryDocumentData(
        val name: String,
        val key: String,
        val features: List<FeatureDocumentData>
    )

    data class FeatureDocumentData(
        val name: String,
        val key: String,
        val settings: SettingDocumentData
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

            // 例: Documentクラスのインスタンスを作成して利用
            val document = Document()
            println("Documentのインスタンスを作成しました: $document")
            val documentData = document.generateData()
        }
    }

    fun generateData(): DocumentData {
        println("${InfiniteClient.featureCategories.size}")
        return DocumentData.empty()
    }
}