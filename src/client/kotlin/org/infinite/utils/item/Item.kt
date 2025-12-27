package org.infinite.utils.item
import net.minecraft.client.Minecraft
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import java.util.Optional
import java.util.function.Function

fun enchantLevel(
    items: ItemStack,
    enchantment: ResourceKey<Enchantment>,
): Int {
    val registryManager = Minecraft.getInstance().level?.registryAccess()
    val enchantRegistry = registryManager?.lookupOrThrow(Registries.ENCHANTMENT)
    if (enchantRegistry != null) {
        val enchantment: Optional<Holder.Reference<Enchantment>> =
            enchantRegistry.get(enchantment)
        val enchantmentLevel =
            enchantment
                .map(
                    Function { entry: Holder.Reference<Enchantment> ->
                        EnchantmentHelper.getItemEnchantmentLevel(
                            entry,
                            items,
                        )
                    },
                ).orElse(0)
        return enchantmentLevel
    } else {
        return 0
    }
}
