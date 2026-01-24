package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature

// 1. ClickGuiScreen に型引数 <Category<*, out Feature>> を追加
class ListGlobalFeatureCategoriesScreen(parent: Screen? = null) : ClickGuiScreen<Category<*, out Feature>>(Component.literal("Infinite Client"), parent) {

    override val categories: List<Category<*, out Feature>>
        get() = InfiniteClient.globalFeatures.categories.values.toList()

    // 2. 抽象メソッド openFeatureSettings を実装
    override fun openFeatureSettings(feature: Feature) {
        // 必要に応じて設定画面を開くロジックをここに記述
    }
}
