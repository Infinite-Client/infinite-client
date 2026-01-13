package org.infinite.infinite.features.local.level.tag

import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.level.esp.EspFeature
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.utils.alpha

class UltraTagFeature : LocalFeature() {
    override val featureType = FeatureType.Utils

    // --- 設定 ---
    private val range by property(IntProperty(64, 16, 256))
    private val showMobs by property(BooleanProperty(true))
    private val showPlayers by property(BooleanProperty(true))
    private val showItems by property(BooleanProperty(true))

    private val fadeStart by property(IntProperty(30, 1, 256))
    private val fadeEnd by property(IntProperty(60, 1, 256))

    override fun onEndUiRendering(graphics2D: Graphics2D): Graphics2D {
        val p = player ?: return graphics2D
        val world = level ?: return graphics2D

        // 1. 指定範囲内のエンティティのみを取得（最適化）
        val boundingBox = p.boundingBox.inflate(range.value.toDouble())
        val candidates = world.getEntities(p, boundingBox) { entity ->
            when (entity) {
                is Player -> showPlayers.value
                is ItemEntity -> showItems.value
                is LivingEntity -> showMobs.value
                else -> false
            }
        }

        // 2. 投影と距離計算
        val renderList = candidates.mapNotNull { entity ->
            val offset = if (entity is LivingEntity) entity.bbHeight.toDouble() + 0.2 else 0.5
            val worldPos = entity.getPosition(graphics2D.gameDelta).add(0.0, offset, 0.0)

            val screenPos = graphics2D.projectWorldToScreen(Vec3(worldPos.x, worldPos.y, worldPos.z))
            if (screenPos != null) {
                val dist = p.distanceTo(entity)
                Triple(entity, screenPos, dist)
            } else {
                null
            }
        }.sortedByDescending { it.third }

        // 3. レンダリング
        renderList.forEach { (entity, pos, dist) ->
            val alpha = calculateAlpha(dist.toDouble())
            if (alpha < 0.05f) return@forEach

            val scale = (1.0f - (dist / range.value.toFloat() * 0.4f)).coerceIn(0.6f, 1.0f)

            graphics2D.push()
            graphics2D.translate(pos.first.toFloat(), pos.second.toFloat())
            graphics2D.scale(scale, scale)

            renderTag(graphics2D, entity, alpha)

            graphics2D.pop()
        }

        return graphics2D
    }

    private fun renderTag(graphics2D: Graphics2D, entity: Entity, alpha: Float) {
        val theme = InfiniteClient.theme
        val alphaInt = (alpha * 255).toInt()
        val name = entity.name.string
        val colorScheme = theme.colorScheme
        val textWidth = (name.length * graphics2D.textStyle.size).coerceIn(40f, 120f)
        val padding = 4f
        val tagWidth = textWidth.coerceAtLeast(42f) + padding * 2
        val tagHeight = if (entity is LivingEntity) 16f else 12f

        val x = -tagWidth / 2f
        val y = -tagHeight

        // 背景 (Theme API)
        theme.renderBackGround(x, y, tagWidth, tagHeight, graphics2D, alpha * 0.8f)

        // ステータスに応じたボーダーカラー
        val borderColor = EspFeature.handleEntityColor(entity).alpha(alphaInt)
        graphics2D.strokeStyle.color = borderColor
        graphics2D.strokeStyle.width = 1.5f
        graphics2D.strokeRect(x, y, tagWidth, tagHeight)

        // 名前描画
        graphics2D.fillStyle = 0xFFFFFFFF.toInt().alpha(alphaInt)
        graphics2D.textCentered(name, x + tagWidth / 2f, y + tagHeight / 2)

        // HPバー & ステータスオーバーレイ
        if (entity is LivingEntity) {
            val healthPer = (entity.health / entity.maxHealth).coerceIn(0f, 1f)
            val barY = y + 11f
            val barWidth = tagWidth - padding * 2

            // HPバー本体
            graphics2D.fillStyle = colorScheme.color(healthPer * 108f, 1.0f, 0.5f, alpha)
            graphics2D.fillRect(x + padding, barY, barWidth * healthPer, 3f)

            // 特殊状態オーバーレイ（燃焼・毒など）
            val overlayColor = getOverlayColor(entity)
            if (overlayColor != null) {
                graphics2D.fillStyle = overlayColor.alpha((alpha * 100).toInt())
                graphics2D.fillRect(x, y, tagWidth, tagHeight) // タグ全体に薄く色を乗せる
            }
        }

        // アイテム表示
        if (entity is ItemEntity) {
            graphics2D.itemCentered(entity.item, 0f, y - 10f, 16f)
        }
    }

    private fun getOverlayColor(entity: LivingEntity): Int? {
        return when {
            entity.isOnFire -> 0xFFFFA500.toInt() // オレンジ
            entity.hasEffect(MobEffects.POISON) -> 0xFF00FF00.toInt() // 緑
            entity.hasEffect(MobEffects.WITHER) -> 0xFF333333.toInt() // 黒
            else -> null
        }
    }

    private fun calculateAlpha(dist: Double): Float {
        val min = 0.2f
        return when {
            dist <= fadeStart.value -> 1f
            dist >= fadeEnd.value -> min
            else -> 1f - ((dist - fadeStart.value) / (fadeEnd.value - fadeStart.value)).toFloat() * (1f - min)
        }.coerceIn(0f, 1f)
    }
}
