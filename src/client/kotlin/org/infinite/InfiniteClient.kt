package org.infinite

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.KeyMapping
import org.infinite.InfiniteClient.feature
import org.infinite.infinite.command.InfiniteCommand
import org.infinite.infinite.features.global.InfiniteGlobalFeatures
import org.infinite.infinite.features.local.InfiniteLocalFeatures
import org.infinite.infinite.theme.default.DefaultTheme
import org.infinite.infinite.theme.infinite.InfiniteTheme
import org.infinite.infinite.ui.screen.LocalFeatureCategoriesScreen
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
    val gameScreenBindingPair: LocalFeature.BindingPair by lazy {
        LocalFeature.BindingPair(
            KeyBindingHelper.registerKeyBinding(
                KeyMapping(
                    "key.infinite.game_options",
                    GLFW.GLFW_KEY_RIGHT_SHIFT,
                    KeyMapping.Category.GAMEPLAY,
                ),
            ),
        ) {
            minecraft.execute {
                minecraft.setScreen(LocalFeatureCategoriesScreen())
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
        get() = globalFeatures.rendering.uiStyleFeature.style.value

    override fun onInitializeClient() {
        LogSystem.init()
        LibInfiniteClient.loadNativeLibrary()
        InfiniteCommand.register()
        themeManager.register(InfiniteTheme())
        // 1. グローバル設定のロード
        ConfigManager.loadGlobal()
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
}
