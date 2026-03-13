package org.infinite.infinite.features.local.inventory

import org.infinite.infinite.features.local.inventory.armor.AutoArmorFeature
import org.infinite.infinite.features.local.inventory.control.ContainerUtilFeature
import org.infinite.infinite.features.local.inventory.relocate.ItemRelocateFeature
import org.infinite.infinite.features.local.inventory.restock.ItemRestockFeature
import org.infinite.libs.core.features.categories.category.LocalCategory

@Suppress("Unused")
class LocalInventoryCategory : LocalCategory() {
    val autoArmorFeature by feature(AutoArmorFeature())
    val containerUtilFeature by feature(ContainerUtilFeature())
    val itemRelocateFeature by feature(ItemRelocateFeature())
    val itemRestockFeature by feature(ItemRestockFeature())
    val swapToolFeature by feature(SwapToolFeature())
}
