package org.infinite.infinite.ui.screen

import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.infinite.ui.widget.LocalCategoryWidget
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.ui.screen.AbstractCarouselScreen
import org.infinite.libs.ui.widgets.AbstractCarouselWidget

class LocalFeatureCategoriesScreen : AbstractCarouselScreen<LocalCategory>(Component.literal("Infinite Client")) {
    override val dataSource: List<LocalCategory>
        get() = InfiniteClient.localFeatures.categories.values.toList()

    override fun createWidget(index: Int, data: LocalCategory): AbstractCarouselWidget<LocalCategory> {
        return LocalCategoryWidget(0, 0, 120, 180, data, this, index)
    }
}
