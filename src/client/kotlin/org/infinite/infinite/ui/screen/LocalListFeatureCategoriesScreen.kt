package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature

// 1. ClickGuiScreen に型引数 <Category<*, out Feature>> を渡す
class LocalListFeatureCategoriesScreen(parent: Screen? = null) : ClickGuiScreen<Category<*, out Feature>>(Component.literal("Infinite Client"), parent) {

    override val categories: List<Category<*, out Feature>>
        get() = InfiniteClient.localFeatures.categories.values.toList()

    // 2. ClickGuiScreen で定義した抽象メソッドを実装する
    override fun openFeatureSettings(feature: Feature) {
        // ここに設定画面を開く処理を記述（現時点では空でもビルドは通ります）
    }
}
