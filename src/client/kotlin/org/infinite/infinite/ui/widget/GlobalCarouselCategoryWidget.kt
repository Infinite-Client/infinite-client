package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.libs.core.features.categories.category.GlobalCategory
import org.infinite.libs.ui.screen.AbstractCarouselScreen

class GlobalCarouselCategoryWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    category: GlobalCategory,
    parent: AbstractCarouselScreen<GlobalCategory>,
    index: Int,
) : CarouselCategoryWidget<GlobalCategory>(
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

        // GlobalCategory に含まれる GlobalFeature をループして Widget を追加
        // data は親の CategoryWidget で定義されている GlobalCategory インスタンスです
        data.features.forEach { (_, feature) ->
            // Global 用の FeatureWidget を作成して追加
            layout.addChild(GlobalCarouselFeatureWidget(0, 0, itemWidth, feature = feature))
        }
    }

    override fun onSelected(data: GlobalCategory) {
        // デバッグログまたは選択時の振る舞い
        println("Selected Global Category: ${data.name} (Translation: ${data.translation()})")
    }
}
