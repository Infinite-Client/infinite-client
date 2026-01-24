package org.infinite.infinite.ui.screen

import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature

class ListGlobalFeatureCategoriesScreen(parent: net.minecraft.client.gui.screens.Screen? = null) : ClickGuiScreen(net.minecraft.network.chat.Component.literal("Infinite Client"), parent) {

    override val categories: List<Category<*, out Feature>>
        get() = InfiniteClient.globalFeatures.categories.values.toList()
}
