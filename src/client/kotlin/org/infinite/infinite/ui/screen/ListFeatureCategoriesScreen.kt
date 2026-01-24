package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.widget.ListCategoryWidget
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import kotlin.reflect.KClass

/**
 * ClickGuiScreen を継承し、リスト形式で機能を表示する画面のベース
 */
abstract class ListFeatureCategoriesScreen<K : KClass<out Feature>, V : Feature, T : Category<K, V>, W : ListCategoryWidget<T>>(
    parent: Screen? = null,
) : ClickGuiScreen<T>(Component.literal("Infinite Client"), parent) {

    /**
     * ClickGuiScreen のカテゴリソースとして、子クラスの dataSource を提供
     */
    abstract val dataSource: List<T>
    override val categories: List<T> get() = dataSource

    /**
     * ClickGuiScreen での「SET」ボタン押下時の動作を定義
     */
    override fun openFeatureSettings(feature: Feature) {
        // ここに設定画面を開くロジックを実装
        // Registryを削除したため、直接インスタンス化するか、別のファクトリを使用します
        // 例: minecraft.setScreen(LocalFeatureSettingsScreen(feature, this))
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // 背景のブラー処理
        try {
            renderBlurredBackground(guiGraphics)
        } catch (ex: IllegalStateException) {
            renderTransparentBackground(guiGraphics)
        }

        // 親クラス (ClickGuiScreen) の描画ロジックを呼び出す
        // これにより、サイドバー、検索ボックス、機能リストが自動的に描画されます
        super.render(guiGraphics, mouseX, mouseY, delta)
    }

    // --- ClickGuiScreen で既に実装されているため、以下のメソッドは削除可能 ---
    // searchBox, updateSearch, mouseClicked, keyPressed, etc...
    // これらは ClickGuiScreen 側で一括管理されます。
}
