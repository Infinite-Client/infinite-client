package org.infinite.infinite.features.local.level.tag

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
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
import kotlin.math.acos

class UltraTagFeature : LocalFeature() {
    override val featureType = FeatureLevel.Utils

    private val range by property(IntProperty(64, 16, 256))
    private val showMobs by property(BooleanProperty(true))
    private val showPlayers by property(BooleanProperty(true))
    private val showItems by property(BooleanProperty(true))
    private val showEquipments by property(BooleanProperty(true))

    private val fadeStart by property(IntProperty(30, 1, 256))
    private val fadeEnd by property(IntProperty(60, 1, 256))

    private val itemHideAngle by property(IntProperty(20, 0, 90))

    override fun onEndUiRendering(graphics2D: Graphics2D) {
        val p = player ?: return
        val world = level ?: return

        val boundingBox = p.boundingBox.inflate(range.value.toDouble())
        val candidates = world.getEntities(p, boundingBox) { entity ->
            when (entity) {
                is Player -> showPlayers.value
                is ItemEntity -> showItems.value
                is LivingEntity -> showMobs.value
                else -> false
            }
        }

        val renderList = candidates.mapNotNull { entity ->
            if (entity is ItemEntity) {
                val lookVec = p.lookAngle
                val entityVec = entity.position().subtract(p.eyePosition).normalize()
                val dot = lookVec.dot(entityVec)
                val angle = Math.toDegrees(acos(dot.coerceIn(-1.0, 1.0)))
                if (angle < itemHideAngle.value) return@mapNotNull null
            }

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
    }

    private fun renderTag(graphics2D: Graphics2D, entity: Entity, alpha: Float) {
        val theme = InfiniteClient.theme
        val alphaInt = (alpha * 255).toInt()
        val colorScheme = theme.colorScheme

        val name = if (entity is Player) entity.name.string else ""
        val hasName = name.isNotEmpty()
        val canHaveHealth = entity is LivingEntity

        val padding = 4f
        val itemSize = 16f
        val itemPadding = 2f

        val textWidth = if (hasName) {
            (name.length * (graphics2D.textStyle.size * 0.8f)).coerceIn(40f, 120f)
        } else {
            if (canHaveHealth) 30f else 16f
        }

        val tagWidth = textWidth + padding * 2
        val tagHeight = when {
            hasName -> 14f
            canHaveHealth -> 6f
            else -> 2f // アイテム等の場合、非常に薄い枠にする
        }

        val x = -tagWidth / 2f
        val y = -tagHeight

        // 1. 背景とボーダー
        if (canHaveHealth) {
            theme.renderBackGround(x, y, tagWidth, tagHeight, graphics2D, alpha * 0.8f)
            val borderColor = EspFeature.handleEntityColor(entity).alpha(alphaInt)
            graphics2D.strokeStyle.color = borderColor
            graphics2D.strokeStyle.width = 1.0f
            graphics2D.strokeRect(x, y, tagWidth, tagHeight)
        }
        // 2. 名前 (Playerのみ)
        if (hasName) {
            graphics2D.fillStyle = colorScheme.foregroundColor.alpha(alphaInt)
            graphics2D.textCentered(name, 0f, y + 4f)
        }

        // 3. HPバー (LivingEntity かつ ItemEntityでない場合のみ)
        if (canHaveHealth) {
            val healthPer = (entity.health / entity.maxHealth).coerceIn(0f, 1f)
            val barHeight = 3f
            val barWidth = tagWidth - 4f
            val barX = x + 2f
            val barY = if (hasName) y + tagHeight - barHeight - 2f else y + 1.5f

            graphics2D.fillStyle = 0x000000.alpha((alphaInt * 0.5f).toInt())
            graphics2D.fillRect(barX, barY, barWidth, barHeight)

            graphics2D.fillStyle = colorScheme.color(healthPer * 108f, 1.0f, 0.5f, alpha)
            graphics2D.fillRect(barX, barY, barWidth * healthPer, barHeight)
        }

        // 4. アイテム・装備
        if (entity is ItemEntity) {
            // アイテム自体の表示（タグの直上）
            graphics2D.itemCentered(entity.item, 0f, y - 10f, 16f)
        } else if (entity is LivingEntity && showEquipments.value) {
            // 手持ちアイテム (左右)
            val mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND)
            val offHand = entity.getItemBySlot(EquipmentSlot.OFFHAND)

            if (!mainHand.isEmpty) {
                graphics2D.item(
                    mainHand,
                    (tagWidth / 2f) + itemPadding,
                    y + (tagHeight / 2f) - (itemSize / 2f),
                    itemSize,
                )
            }
            if (!offHand.isEmpty) {
                graphics2D.item(
                    offHand,
                    (-tagWidth / 2f) - itemSize - itemPadding,
                    y + (tagHeight / 2f) - (itemSize / 2f),
                    itemSize,
                )
            }

            // 防具 (タグの上側に変更)
            val armorSlots = listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
            val totalArmorWidth = (itemSize * 4) + (itemPadding * 3)
            var armorX = -totalArmorWidth / 2f
            // タグのさらに上側に配置 (名前がある場合はそのさらに上)
            val armorY = y - itemSize - itemPadding

            armorSlots.forEach { slot ->
                val armorStack = entity.getItemBySlot(slot)
                if (!armorStack.isEmpty) {
                    graphics2D.item(armorStack, armorX, armorY, itemSize)
                }
                armorX += itemSize + itemPadding
            }
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
