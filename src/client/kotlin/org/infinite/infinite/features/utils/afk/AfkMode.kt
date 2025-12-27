package org.infinite.infinite.features.utils.afk

import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting

class AfkMode : ConfigurableFeature(initialEnabled = false) {
    override val settings: List<FeatureSetting<*>> = listOf()
    private var hp = 0f

    override fun onTick() {
        val currentHp = player!!.health
        if (currentHp > hp) {
            hp = currentHp
        }
        if (currentHp < hp) {
            InfiniteClient.warn(Component.translatable("afkmode.damage_detected").string)
            disable()
        }
    }

    override fun onEnabled() {
        hp = player!!.health
    }
}
