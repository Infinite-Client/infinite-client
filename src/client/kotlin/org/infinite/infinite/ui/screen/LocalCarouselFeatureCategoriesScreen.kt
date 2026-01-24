package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.screens.Screen
import org.infinite.InfiniteClient
import org.infinite.infinite.ui.widget.LocalCarouselCategoryWidget
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.core.features.feature.LocalFeature
import kotlin.reflect.KClass

class LocalCarouselFeatureCategoriesScreen(parent: Screen? = null) :
    CarouselFeatureCategoriesScreen<
        KClass<out LocalFeature>,
        LocalFeature,
        LocalCategory,
        LocalCarouselCategoryWidget,
        >(parent) {

    override val dataSource: List<LocalCategory>
        get() = InfiniteClient.localFeatures.categories.values.toList()

    override fun createWidget(
        index: Int,
        data: LocalCategory,
    ): LocalCarouselCategoryWidget = LocalCarouselCategoryWidget(0, 0, 120, 180, data, this, index)
}
