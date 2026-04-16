package org.infinite

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.Screen
import org.infinite.infinite.command.InfiniteCommand
import org.infinite.infinite.features.global.InfiniteGlobalFeatures
import org.infinite.infinite.features.local.InfiniteLocalFeatures
import org.infinite.infinite.theme.default.DefaultTheme
import org.infinite.infinite.theme.infinite.InfiniteTheme
import org.infinite.infinite.theme.stylish.StylishTheme
import org.infinite.infinite.ui.screen.GlobalCarouselFeatureCategoriesScreen
import org.infinite.infinite.ui.screen.GlobalListFeatureCategoriesScreen
import org.infinite.infinite.ui.screen.LocalCarouselFeatureCategoriesScreen
import org.infinite.infinite.ui.screen.LocalListFeatureCategoriesScreen
import org.infinite.libs.addon.InfiniteAddon
import org.infinite.libs.config.ConfigManager
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.core.features.categories.category.GlobalCategory
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.core.features.feature.GlobalFeature
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.tick.GameTicks
import org.infinite.libs.core.tick.SystemTicks
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.log.LogSystem
import org.infinite.libs.rust.LibInfiniteClient
import org.infinite.libs.translation.TranslationChecker
import org.infinite.libs.ui.style.UiStyle
import org.infinite.libs.ui.theme.Theme
import org.infinite.libs.ui.theme.ThemeManager
import org.lwjgl.glfw.GLFW
import kotlin.reflect.KClass

object InfiniteClient : MinecraftInterface(), ClientModInitializer {
    val globalFeatures = InfiniteGlobalFeatures()
    val localFeatures = InfiniteLocalFeatures()

    val featureCategories get() = localFeatures
    val globalFeatureCategories get() = globalFeatures

    fun genTranslations(rootPath: String = ".") {
        var langDir = java.io.File(rootPath, "infinite-client/src/main/resources/assets/infinite-client/lang")
        if (!langDir.exists()) {
            langDir = java.io.File(rootPath, "src/main/resources/assets/infinite-client/lang")
        }
        if (!langDir.exists()) return

        val keys = mutableSetOf<String>()
        // Collect keys from local features
        localFeatures.categories.values.forEach { category ->
            keys.add(category.translation())
            category.features.values.forEach { feature ->
                keys.add(feature.translation())
                feature.properties.values.forEach { property ->
                    property.translationKey()?.let { keys.add(it) }
                }
            }
        }
        // Collect keys from global features
        globalFeatures.categories.values.forEach { category ->
            keys.add(category.translation())
            category.features.values.forEach { feature ->
                keys.add(feature.translation())
                feature.properties.values.forEach { property ->
                    property.translationKey()?.let { keys.add(it) }
                }
            }
        }

        // Add standard documentation keys used in Document.kt
        keys.add("doc.infinite.properties_section_title")
        keys.add("doc.infinite.property_info_title")
        keys.add("doc.infinite.property_type")
        keys.add("doc.infinite.property_default")
        keys.add("doc.infinite.property_min")
        keys.add("doc.infinite.property_max")
        keys.add("doc.infinite.property_options")
        keys.add("doc.infinite.property_list_count")
        keys.add("doc.infinite.property_list_type")

        // Keymappings
        localFeatures.categories.values.forEach { category ->
            category.features.values.forEach { feature ->
                keys.addAll(feature.getActionTranslationKeys())
            }
        }
        keys.add("key.infinite.game_options")

        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        val langFiles = langDir.listFiles { _, name -> name.endsWith(".json") } ?: return

        for (file in langFiles) {
            val type = object : com.google.gson.reflect.TypeToken<MutableMap<String, String>>() {}.type
            val currentTranslations: MutableMap<String, String> = try {
                gson.fromJson(file.readText(java.nio.charset.StandardCharsets.UTF_8), type) ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }

            var changed = false
            for (key in keys) {
                if (!currentTranslations.containsKey(key)) {
                    currentTranslations[key] = key
                    changed = true
                }
            }

            if (changed) {
                val sortedMap = currentTranslations.toSortedMap()
                file.writeText(gson.toJson(sortedMap), java.nio.charset.StandardCharsets.UTF_8)
                println("Updated translation file: ${file.name} with ${keys.size} keys.")
            }
        }
    }

    val gameScreenMappingPair: LocalFeature.MappingPair by lazy {
        LocalFeature.MappingPair(
            KeyMappingHelper.registerKeyMapping(
                KeyMapping(
                    "key.infinite.game_options",
                    GLFW.GLFW_KEY_RIGHT_SHIFT,
                    KeyMapping.Category.GAMEPLAY,
                ),
            ),
        ) {
            minecraft.execute {
                // 現時点での要望: 直接 Carousel を開く
                // もし「設定に従いたい」場合は dev 側のロジック（後述）を使用してください
                val screen = when (uiStyle) {
                    UiStyle.Simple -> LocalListFeatureCategoriesScreen()
                    UiStyle.Carousel -> LocalCarouselFeatureCategoriesScreen()
                }
                minecraft.setScreen(
                    screen,
                )
            }
        }
    }

    private fun loadAddons() {
        val loader = net.fabricmc.loader.api.FabricLoader.getInstance()
        // fabric.mod.json の "entrypoint" -> "infinite_addon" を探す
        val containers = loader.getEntrypointContainers("infinite_addon", InfiniteAddon::class.java)
        for (container in containers) {
            val modId = container.provider.metadata.id
            try {
                val addon = container.entrypoint
                addon.onInitializeAddon(this)
                LogSystem.info("Successfully loaded Infinite addon from: $modId")
            } catch (e: Exception) {
                LogSystem.error("Failed to load Infinite addon from mod: $modId, $e")
            }
        }
    }

    /**
     * ハッシュマップを使用して $O(1)$ でFeatureインスタンスを取得します。
     * * @param category 検索対象のカテゴリクラス (Key)
     * @param feature 検索対象の機能クラス (Key)
     * @throws IllegalArgumentException カテゴリが登録されていない場合
     */
    @Suppress("UNCHECKED_CAST")
    fun <C : Category<*, *>, F : Feature> feature(category: KClass<C>, feature: KClass<out F>): F? = when {
        GlobalCategory::class.java.isAssignableFrom(category.java) -> {
            val globalCategory = globalFeatures.getCategory(category as KClass<out GlobalCategory>)
            if (GlobalFeature::class.java.isAssignableFrom(feature.java)) {
                globalCategory?.getFeature(feature as KClass<out GlobalFeature>) as? F
            } else {
                null
            }
        }

        LocalCategory::class.java.isAssignableFrom(category.java) -> {
            val localCategory = localFeatures.getCategory(category as KClass<out LocalCategory>)
            if (LocalFeature::class.java.isAssignableFrom(feature.java)) {
                localCategory?.getFeature(feature as KClass<out LocalFeature>) as? F
            } else {
                null
            }
        }

        else -> null
    }

    val worldTicks = GameTicks(localFeatures)
    val themeManager: ThemeManager = ThemeManager(DefaultTheme())
    val theme: Theme
        get() {
            val themeName = globalFeatures.rendering.themeFeature.currentTheme.value
            return themeManager.getTheme(themeName)
        }
    val uiStyle: UiStyle
        get() = globalFeatures.rendering.themeFeature.style.value

    override fun onInitializeClient() {
        LogSystem.init()
        LibInfiniteClient.loadNativeLibrary()
        loadAddons()
        InfiniteCommand.register()
        themeManager.register(InfiniteTheme())
        themeManager.register(StylishTheme())
        // 1. グローバル設定のロード
        ConfigManager.loadGlobal()
        globalFeatures.rendering.themeFeature.enable()
        globalFeatures.rendering.infiniteLoadingFeature.enable()
        globalFeatures.onInitialized()
        TranslationChecker.register()
        localFeatures.registerAllActions()
        // サーバー接続時 (ログイン成功後)
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            // 2. ローカル設定（サーバー/ワールド別）のロード
            ConfigManager.loadLocal()
            localFeatures.onConnected()
        }

        // サーバー切断時
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            // 3. ローカル設定の保存（切断時にそのサーバーの状態を保持）
            ConfigManager.saveLocal()
            localFeatures.onDisconnected()
        }

        // --- Tick Events ---
        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            globalFeatures.onStartTick()
        }
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            globalFeatures.onEndTick()
        }

        worldTicks.register()
        SystemTicks.register()
        // --- Shutdown (マイクラ終了時) ---
        ClientLifecycleEvents.CLIENT_STOPPING.register { _ ->
            // 4. すべての設定を最終保存
            ConfigManager.saveGlobal()
            ConfigManager.saveLocal() // 念のため現在の接続先も保存

            globalFeatures.onShutdown()
            localFeatures.onShutdown()
        }
    }

    fun handleOpenGlobalSettingScreen(parent: Screen) {
        val screen = when (uiStyle) {
            UiStyle.Simple -> GlobalListFeatureCategoriesScreen(parent)
            UiStyle.Carousel -> GlobalCarouselFeatureCategoriesScreen(parent)
        }
        minecraft.setScreen(screen)
    }
}
