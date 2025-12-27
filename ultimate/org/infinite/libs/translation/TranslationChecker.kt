package org.infinite.libs.translation

import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.minecraft.locale.Language
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import org.infinite.UltimateClient
import org.infinite.libs.log.LogSystem
import java.util.LinkedList

object TranslationChecker : SimplePreparableReloadListener<Unit>() {
    private val additionalTranslationList: LinkedList<String> = LinkedList()
    fun add(id: String) {
        additionalTranslationList.add(id)
    }

    private val allTranslations: List<String>
        get() {
            val localFeatures = UltimateClient.localFeatures.translations
            val globalFeatures = UltimateClient.globalFeatures.translations
            return localFeatures + globalFeatures + additionalTranslationList
        }

    fun check(): List<String> {
        val language = Language.getInstance()
        return allTranslations.filter { key ->
            !language.has(key)
        }
    }

    // Identifier/ResourceLocation の生成 (1.21相当の記述)
    private val ID = Identifier.fromNamespaceAndPath("infinite", "translation_checker")

    /**
     * リソースのリロードが完了した際にメインスレッドで実行されます
     */
    override fun apply(data: Unit, manager: ResourceManager, profiler: ProfilerFiller) {
        val missing = check()
        if (missing.isNotEmpty()) {
            LogSystem.warn("Missing translations found: ${missing.joinToString(", ")}")
        }
    }

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller) {
        // 同期処理のみの場合は空でOK
    }

    /**
     * ClientModInitializerなどで呼び出す登録用メソッド
     */
    fun register() {
        // v1 ResourceLoader を使用してリローダーを登録
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(ID, this)
    }
}
