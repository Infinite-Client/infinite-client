package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.ui.screen.AbstractCarouselScreen

class LocalCarouselCategoryWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    category: LocalCategory, // プロパティとして保持しなくても、親の 'data' を利用可能
    parent: AbstractCarouselScreen<LocalCategory>,
    index: Int,
) : CarouselCategoryWidget<LocalCategory>(
    x,
    y,
    width,
    height,
    category,
    parent,
    index,
    Component.translatable(category.translation()),
) {

    override fun buildContent(layout: LinearLayout, width: Int) {
        val innerSpacing = 5
        val itemWidth = width - 2 * innerSpacing

        // 親クラスの 'data' (LocalCategory) にある features をループ
        data.features.forEach { (_, feature) ->
            layout.addChild(LocalCarouselFeatureWidget(0, 0, itemWidth, feature = feature))
        }
    }

    override fun onSelected(data: LocalCategory) {
        println("Selected Category: ${data.name} (Translation: ${data.translation()})")
    }
}
