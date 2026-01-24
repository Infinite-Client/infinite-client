package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.screens.Screen
import org.infinite.InfiniteClient
import org.infinite.infinite.ui.widget.GlobalCarouselCategoryWidget
import org.infinite.libs.core.features.categories.category.GlobalCategory
import org.infinite.libs.core.features.feature.GlobalFeature
import kotlin.reflect.KClass

class GlobalCarouselFeatureCategoriesScreen(parent: Screen? = null) :
    CarouselFeatureCategoriesScreen<
        KClass<out GlobalFeature>,
        GlobalFeature,
        GlobalCategory,
        GlobalCarouselCategoryWidget,
        >(parent) {

    // Globalのカテゴリデータソースを取得
    override val dataSource: List<GlobalCategory>
        get() = InfiniteClient.globalFeatures.categories.values.toList()

    // Global用のWidgetをインスタンス化
    override fun createWidget(
        index: Int,
        data: GlobalCategory,
    ): GlobalCarouselCategoryWidget = GlobalCarouselCategoryWidget(0, 0, 120, 180, data, this, index)
}
