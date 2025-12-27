package org.infinite.infinite.features.rendering.detailinfo

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import org.infinite.libs.graphics.Graphics2D
import org.infinite.utils.rendering.alpha

object EntityContentRenderer {
    private const val PADDING = 5

    fun calculateHeight(
        client: Minecraft,
        detail: DetailInfo.TargetDetail.EntityDetail,
        uiWidth: Int,
    ): Int {
        val font = client.font
        var requiredHeight = DetailInfoRenderer.BORDER_WIDTH + PADDING + font.lineHeight + PADDING

        val entity = detail.entity
        if (entity is LivingEntity) {
            // Health bar
            requiredHeight += DetailInfoRenderer.BAR_HEIGHT + DetailInfoRenderer.BAR_PADDING

            // Equipment section
            requiredHeight += font.lineHeight + PADDING // Header
            val equipmentCount = getEquipmentCount(entity)
            requiredHeight += equipmentCount * font.lineHeight + if (equipmentCount > 0) PADDING else 0

            // Effects section
            requiredHeight += font.lineHeight + PADDING // Header
            val effects = entity.activeEffects
            requiredHeight += effects.size * font.lineHeight + if (effects.isNotEmpty()) PADDING else 0

            // Armor value
            requiredHeight += font.lineHeight + PADDING
        }

        // Position at the bottom
        requiredHeight += font.lineHeight + DetailInfoRenderer.BORDER_WIDTH + PADDING
        requiredHeight += font.lineHeight + PADDING
        return requiredHeight
    }

    fun draw(
        graphics2d: Graphics2D,
        client: Minecraft,
        detail: DetailInfo.TargetDetail.EntityDetail,
        startX: Int,
        startY: Int,
        uiWidth: Int,
    ) {
        val font = client.font
        var currentY = startY + DetailInfoRenderer.BORDER_WIDTH + PADDING

        // Entity name and ID
        val textX = startX + DetailInfoRenderer.BORDER_WIDTH + PADDING
        val entityName = detail.entity.type.description.string
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(detail.entity.type).toString()
        graphics2d.drawText(
            entityName,
            textX,
            currentY,
            org.infinite.InfiniteClient
                .theme()
                .colors.foregroundColor,
            true,
        )
        val nameWidth = font.width(entityName)
        graphics2d.drawText(
            "($entityId)",
            textX + nameWidth + 5,
            currentY,
            org.infinite.InfiniteClient
                .theme()
                .colors.foregroundColor
                .alpha(192),
            true,
        )
        currentY += font.lineHeight + PADDING

        val entity = detail.entity
        if (entity is LivingEntity) {
            currentY += DetailInfoRenderer.BAR_HEIGHT + DetailInfoRenderer.BAR_PADDING

            // Equipment
            graphics2d.drawText(
                "Equipment:",
                textX,
                currentY,
                org.infinite.InfiniteClient
                    .theme()
                    .colors.foregroundColor,
                true,
            )
            currentY += font.lineHeight + PADDING
            currentY = drawEquipment(graphics2d, font, entity, textX + PADDING, currentY)

            // Status Effects
            graphics2d.drawText(
                "Status Effects:",
                textX,
                currentY,
                org.infinite.InfiniteClient
                    .theme()
                    .colors.foregroundColor,
                true,
            )
            currentY += font.lineHeight + PADDING
            currentY = drawEffects(graphics2d, font, entity.activeEffects, textX + PADDING, currentY)

            // Armor
            val armor = entity.armorValue
            graphics2d.drawText(
                "Armor: $armor",
                textX,
                currentY,
                org.infinite.InfiniteClient
                    .theme()
                    .colors.foregroundColor,
                true,
            )
            currentY += font.lineHeight + PADDING
        }

        // Position
        val infoPos = detail.entity.blockPosition()
        val posText = "Pos: x=${infoPos.x}, y=${infoPos.y}, z=${infoPos.z}"
        graphics2d.drawText(
            posText,
            textX,
            currentY,
            org.infinite.InfiniteClient
                .theme()
                .colors.foregroundColor,
            true,
        )
    }

    private fun getEquipmentCount(entity: LivingEntity): Int {
        var count = 0
        if (!entity.mainHandItem.isEmpty) count++
        if (!entity.offhandItem.isEmpty) count++
        EquipmentSlot.entries
            .filter {
                it in
                    listOf(
                        EquipmentSlot.HEAD,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.FEET,
                    )
            }.forEach {
                if (!entity.getItemBySlot(it).isEmpty) count++
            }
        return count
    }

    private fun drawEquipment(
        graphics2d: Graphics2D,
        font: Font,
        entity: LivingEntity,
        x: Int,
        y: Int,
    ): Int {
        var currentY = y
        if (!entity.mainHandItem.isEmpty) {
            graphics2d.drawText(
                "Main Hand: ${entity.mainHandItem.hoverName.string}",
                x,
                currentY,
                org.infinite.InfiniteClient
                    .theme()
                    .colors.foregroundColor,
                true,
            )
            currentY += font.lineHeight
        }
        if (!entity.offhandItem.isEmpty) {
            graphics2d.drawText(
                "Off Hand: ${entity.offhandItem.hoverName.string}",
                x,
                currentY,
                org.infinite.InfiniteClient
                    .theme()
                    .colors.foregroundColor,
                true,
            )
            currentY += font.lineHeight
        }
        EquipmentSlot.entries
            .filter {
                it in
                    listOf(
                        EquipmentSlot.HEAD,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.FEET,
                    )
            }.forEach {
                val stack = entity.getItemBySlot(it)
                if (!stack.isEmpty) {
                    graphics2d.drawText(
                        "${it.name.lowercase().replaceFirstChar { equipment -> equipment.uppercase() }}: ${stack.hoverName.string}",
                        x,
                        currentY,
                        org.infinite.InfiniteClient
                            .theme()
                            .colors.foregroundColor,
                        true,
                    )
                    currentY += font.lineHeight
                }
            }
        return if (getEquipmentCount(entity) > 0) currentY + PADDING else currentY
    }

    private fun drawEffects(
        graphics2d: Graphics2D,
        font: Font,
        effects: Collection<MobEffectInstance>,
        x: Int,
        y: Int,
    ): Int {
        var currentY = y
        effects.forEach { effect ->
            val effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.effect.value())?.path ?: "Unknown"
            val effectName = effectId.replaceFirstChar { it.uppercase() }
            val duration = effect.duration / 20 // in seconds
            val amplifier = effect.amplifier + 1
            graphics2d.drawText(
                "$effectName $amplifier (${duration}s)",
                x,
                currentY,
                org.infinite.InfiniteClient
                    .theme()
                    .colors.foregroundColor,
                true,
            )
            currentY += font.lineHeight
        }
        return if (effects.isNotEmpty()) currentY + PADDING else currentY
    }
}
