package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.widget.CategoryWidget
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.ui.screen.AbstractCarouselScreen
import kotlin.reflect.KClass

abstract class FeatureCategoriesScreen<K : KClass<out Feature>, V : Feature, T : Category<K, V>, W : CategoryWidget<T>>(
    parent: Screen? = null,
) : AbstractCarouselScreen<T>(Component.literal("Infinite Client"), parent) {

    abstract override val dataSource: List<T>

    override val lerpFactor: Float
        get() = 0.8f

    // 戻り値を AbstractCarouselWidget<T> ではなく W にする
    abstract override fun createWidget(index: Int, data: T): W
}
