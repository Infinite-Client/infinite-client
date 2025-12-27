package org.infinite.libs.client.player

import net.minecraft.core.Holder
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.LivingEntity

fun LivingEntity.effectLevel(statusEffect: Holder<MobEffect>): Int {
    val target =
        this.activeEffects.find {
            it.effect == statusEffect
        } ?: return 0
    return target.amplifier
}
