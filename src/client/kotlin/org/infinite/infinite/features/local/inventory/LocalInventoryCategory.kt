package org.infinite.infinite.features.local.inventory

import org.infinite.infinite.features.local.inventory.restock.RestockFeature
import org.infinite.libs.core.features.categories.category.LocalCategory

@Suppress("Unused")
class LocalInventoryCategory : LocalCategory() {
    val restockFeature by feature(RestockFeature())
}
