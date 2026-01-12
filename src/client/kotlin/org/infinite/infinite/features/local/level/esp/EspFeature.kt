package org.infinite.infinite.features.local.level.esp

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.state.EntityRenderState
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
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.number.IntProperty

class EspFeature : LocalFeature() {
    override val featureType = FeatureType.Cheat

    private val playerEsp by property(BooleanProperty(true))
    private val mobEsp by property(BooleanProperty(true))
    private val itemEsp by property(BooleanProperty(true))

    // 透過が始まる距離（これより近いと不透明）
    private val alphaMinDistance by property(IntProperty(8, 0, 16))

    // 完全に透過（最小値）になる距離
    private val alphaMaxDistance by property(IntProperty(64, 32, 128))

    // 遠距離時でも維持する最小の透明度 (0.0 - 1.0)
    private val minAlphaPercent by property(FloatProperty(0.1f, 0.0f, 1.0f))

    fun isShouldApply(entity: Entity): Boolean {
        return when (entity) {
            is Player -> playerEsp.value
            is ItemEntity, is ExperienceOrb -> itemEsp.value
            else -> mobEsp.value
        }
    }

    /**
     * 距離に基づいて色に透過（Alpha）を適用する
     */
    private fun applyDistanceAlpha(entity: Entity, baseColor: Int): Int {
        val mc = Minecraft.getInstance()
        val cameraEntity = mc.cameraEntity ?: return baseColor

        // 1. 距離の計算
        val distance = entity.distanceTo(cameraEntity)

        // 2. 透過率の計算
        val maxDist = alphaMaxDistance.value.toFloat()
        val minDist = alphaMinDistance.value.toFloat()

        // 距離に応じた線形補間 (1.0 -> 遠くなるほど minAlphaPercent に近づく)
        val alphaFactor = 1.0f - ((distance - minDist) / (maxDist - minDist)).coerceIn(0.0f, 1.0f)

        // 最終的なアルファ値を計算（最小値を考慮）
        val finalAlphaFactor = alphaFactor.coerceAtLeast(minAlphaPercent.value)
        val alpha = (finalAlphaFactor * 255).toInt()

        // 3. ARGB形式で合成 (baseColorは 0xRRGGBB であると想定)
        return (alpha shl 24) or (baseColor and 0xFFFFFF)
    }

    /**
     * EntityRendererMixin から呼ばれる。発光色を決定する。
     */
    fun handleEntityColor(entity: Entity, renderState: EntityRenderState): Int {
        val colorScheme = InfiniteClient.theme.colorScheme

        // ベースとなる色の決定
        val baseColor = when (entity) {
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

        // 距離による透過処理を適用
        return applyDistanceAlpha(entity, baseColor)
    }
}
