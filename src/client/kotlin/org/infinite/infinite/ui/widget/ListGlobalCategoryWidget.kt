package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.libs.core.features.categories.category.GlobalCategory
import org.infinite.libs.ui.screen.AbstractCarouselScreen

class ListGlobalCategoryWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    category: GlobalCategory,
    parent: AbstractCarouselScreen<GlobalCategory>,
    index: Int,
) : CategoryWidget<GlobalCategory>(
    x,
    y,
    width,
    height,
    category,
    parent,
    index,
    Component.literal(category.name),
) {

    override fun buildContent(layout: LinearLayout, width: Int, query: String) {
        val innerSpacing = 8
        val itemWidth = width - 2 * innerSpacing
        val normalized = query.trim()

        // GlobalCategory に含まれる GlobalFeature をループして Widget を追加
        data.features.forEach { (_, feature) ->
            val matches = normalized.isEmpty() ||
                feature.name.contains(normalized, ignoreCase = true)
            if (matches) {
                layout.addChild(GlobalFeatureWidget(0, 0, itemWidth, feature = feature))
            }
        }
    }

    override fun onSelected(data: GlobalCategory) {
        // デバッグログまたは選択時の振る舞い
        println("Selected Global Category: ${data.name}")
    }
}
