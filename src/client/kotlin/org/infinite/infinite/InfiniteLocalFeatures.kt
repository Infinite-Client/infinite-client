package org.infinite.infinite

import org.infinite.infinite.features.local.combat.LocalCombatCategory
import org.infinite.infinite.features.local.inventory.LocalInventoryCategory
import org.infinite.infinite.features.local.level.LocalLevelCategory
import org.infinite.infinite.features.local.movement.LocalMovementCategory
import org.infinite.infinite.features.local.rendering.LocalRenderingCategory
import org.infinite.libs.core.features.categories.LocalFeatureCategories

@Suppress("Unused")
class InfiniteLocalFeatures : LocalFeatureCategories() {
    val rendering by category(LocalRenderingCategory())
    val combat by category(LocalCombatCategory())
    val level by category(LocalLevelCategory())
    val inventory by category(LocalInventoryCategory())
    val movement by category(LocalMovementCategory())
}
