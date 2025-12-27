package org.infinite.utils.block

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity

fun AbstractFurnaceBlockEntity.createFuelTimeMap(): Map<Item, Int> {
    val fuelRegistry = Minecraft.getInstance().level?.fuelValues() ?: return mapOf()
    val fuelMap = mutableMapOf<Item, Int>()

    // Iterate over all registered items
    for (item in BuiltInRegistries.ITEM) {
        val fuelTicks = fuelRegistry.burnDuration(ItemStack(item))
        if (fuelTicks > 0) {
            fuelMap[item] = fuelTicks
        }
    }

    // Add special case for bucket (if not already included)
    if (!fuelMap.containsKey(Items.BUCKET)) {
        val bucketTicks = fuelRegistry.burnDuration(ItemStack(Items.BUCKET))
        if (bucketTicks > 0) {
            fuelMap[Items.BUCKET] = bucketTicks
        }
    }

    return fuelMap
}
