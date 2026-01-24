package org.infinite.libs.item

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper

/**
 * 指定されたItemStackから、特定のエンチャントレベルを取得します。
 */
fun enchantLevel(
    stack: ItemStack,
    enchantmentKey: ResourceKey<Enchantment>,
): Int {
    if (stack.isEmpty) return 0

    // レジストリマネージャーの取得
    val registryAccess = Minecraft.getInstance().level?.registryAccess() ?: return 0
    val enchantmentRegistry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT)

    // キーから Holder (RegistryEntry) を取得
    val enchantmentHolder = enchantmentRegistry.get(enchantmentKey).orElse(null) ?: return 0

    // EnchantmentHelperを使用してレベルを取得 (Data Component API対応)
    return EnchantmentHelper.getItemEnchantmentLevel(enchantmentHolder, stack)
}
