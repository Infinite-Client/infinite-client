package org.infinite

import net.fabricmc.api.ClientModInitializer
import org.infinite.libs.core.features.categories.GlobalFeatureCategories
import org.infinite.libs.core.features.categories.LocalFeatureCategories

object UltimateClient : ClientModInitializer {
    val globalFeatureCategories: GlobalFeatureCategories = GlobalFeatureCategories()
    val localFeatureCategories: LocalFeatureCategories = LocalFeatureCategories()

    override fun onInitializeClient() {
        globalFeatureCategories.onInitialized()
    }
}
