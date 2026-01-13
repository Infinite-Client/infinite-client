package org.infinite.infinite.features.local.level.esp

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.npc.Npc
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Rarity
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty

class EspFeature : LocalFeature() {
    override val featureType = FeatureType.Cheat

    private val playerEsp by property(BooleanProperty(true))
    private val mobEsp by property(BooleanProperty(true))
    private val itemEsp by property(BooleanProperty(true))

    fun isShouldApply(entity: Entity): Boolean {
        return when (entity) {
            is Player -> playerEsp.value
            is ItemEntity, is ExperienceOrb -> itemEsp.value
            else -> mobEsp.value
        }
    }

    /**
     * EntityRendererMixin から呼ばれる。発光色を決定する。
     */
    companion object {
        fun handleEntityColor(entity: Entity): Int {
            val colorScheme = InfiniteClient.theme.colorScheme

            // ベースとなる色の決定
            return when (entity) {
                is Player -> colorScheme.accentColor
                is ItemEntity -> {
                    when (entity.item.rarity) {
                        Rarity.COMMON -> colorScheme.whiteColor
                        Rarity.UNCOMMON -> colorScheme.yellowColor
                        Rarity.RARE -> colorScheme.cyanColor
                        Rarity.EPIC -> colorScheme.magentaColor
                    }
                }

                is Enemy -> colorScheme.redColor
                is Animal, is Npc -> colorScheme.greenColor
                else -> colorScheme.yellowColor
            }
        }
    }
}
