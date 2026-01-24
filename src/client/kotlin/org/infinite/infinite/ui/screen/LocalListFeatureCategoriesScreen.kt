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
}
