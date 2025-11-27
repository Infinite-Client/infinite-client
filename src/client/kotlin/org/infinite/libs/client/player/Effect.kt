package org.infinite.libs.client.player

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.entry.RegistryEntry

fun LivingEntity.effectLevel(statusEffect: RegistryEntry<StatusEffect>): Int {
    val target =
        this.statusEffects.find {
            it.effectType == statusEffect
        } ?: return 0
    return target.amplifier
}
