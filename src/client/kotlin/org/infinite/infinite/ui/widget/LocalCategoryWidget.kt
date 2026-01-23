package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.ui.screen.AbstractCarouselScreen

class LocalCategoryWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    category: LocalCategory, // プロパティとして保持しなくても、親の 'data' を利用可能
    parent: AbstractCarouselScreen<LocalCategory>,
    index: Int,
) : CategoryWidget<LocalCategory>(
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

        data.features.forEach { (_, feature) ->
            val matches = normalized.isEmpty() ||
                feature.name.contains(normalized, ignoreCase = true)
            if (matches) {
                layout.addChild(LocalFeatureWidget(0, 0, itemWidth, feature = feature))
            }
        }
    }

    override fun onSelected(data: LocalCategory) {
        println("Selected Category: ${data.name}")
    }
}
