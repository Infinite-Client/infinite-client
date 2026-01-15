package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.screens.Screen
import org.infinite.InfiniteClient
import org.infinite.infinite.ui.widget.LocalCategoryWidget
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.core.features.feature.LocalFeature
import kotlin.reflect.KClass

class LocalFeatureCategoriesScreen(parent: Screen? = null) :
    FeatureCategoriesScreen<
        KClass<out LocalFeature>,
        LocalFeature,
        LocalCategory,
        LocalCategoryWidget,
        >(parent) {

    override val dataSource: List<LocalCategory>
        get() = InfiniteClient.localFeatures.categories.values.toList()

    override fun createWidget(
        index: Int,
        data: LocalCategory,
    ): LocalCategoryWidget = LocalCategoryWidget(0, 0, 120, 180, data, this, index)
}
