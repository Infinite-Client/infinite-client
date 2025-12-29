package org.infinite.infinite.ui.widget

import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.ui.screen.AbstractCarouselScreen
import org.infinite.libs.ui.widgets.AbstractCarouselWidget

class LocalCategoryWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    category: LocalCategory,
    parent: AbstractCarouselScreen<LocalCategory>,
    index: Int,
) : AbstractCarouselWidget<LocalCategory>(x, y, width, height, category, parent, index, Component.translatable(category.translation())) {

    override fun renderCustom(graphics2D: AbstractCarouselScreen.WidgetGraphics2D): AbstractCarouselScreen.WidgetGraphics2D {
        val alpha = ((System.currentTimeMillis() - spawnTime).toFloat() / animationDuration * 0.5f).coerceIn(0f, 0.5f)
        InfiniteClient.theme.renderBackGround(0f, 0f, graphics2D.width.toFloat(), graphics2D.height.toFloat(), graphics2D, alpha)
        return graphics2D
    }

    override fun onSelected(data: LocalCategory) {
        println("Selected: ${data.translation()}")
    }
}
