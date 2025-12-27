package org.infinite.infinite.features.rendering.sensory.esp

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState
import net.minecraft.util.Mth
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.infinite.infinite.features.rendering.sensory.ExtraSensory
import org.infinite.libs.graphics.Graphics3D // Graphics3D をインポート
import org.infinite.libs.graphics.render.RenderUtils
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object ItemEsp {
    private fun itemEntities(): List<ItemEntity> {
        return Minecraft
            .getInstance()
            .level
            ?.entitiesForRendering()
            ?.filter {
                it is ItemEntity
            }?.map {
                it as ItemEntity
            }
            ?: return emptyList()
    }

    /**
     * Graphics3D を利用してアイテムエンティティのアウトラインを描画します。
     *
     * @param graphics3d 描画コンテキスト
     */
    fun render(
        graphics3d: Graphics3D,
        value: ExtraSensory.Method,
    ) {
        if (value == ExtraSensory.Method.OutLine) return
        val tickProgress = graphics3d.tickProgress
        val expand = 0.1

        val items = itemEntities()
        val renderBoxes =
            items.map {
                RenderUtils.ColorBox(
                    rarityColor(it),
                    itemBox(it, tickProgress)
                        .move(0.0, expand, 0.0)
                        .inflate(expand),
                )
            }

        // Graphics3D のラッパーメソッドを利用し、MatrixStack の管理を Graphics3D に任せる
        // vcp.draw() は graphics3d.render() で最後に実行されるため、ここでは不要
        graphics3d.renderLinedColorBoxes(renderBoxes, true)
    }

    fun rarityColor(stack: ItemStack): Int =
        when (stack.rarity) {
            Rarity.COMMON -> {
                org.infinite.InfiniteClient
                    .theme()
                    .colors.foregroundColor
            }

            Rarity.UNCOMMON -> {
                org.infinite.InfiniteClient
                    .theme()
                    .colors.yellowAccentColor
            }

            Rarity.RARE -> {
                org.infinite.InfiniteClient
                    .theme()
                    .colors.aquaAccentColor
            }

            Rarity.EPIC -> {
                org.infinite.InfiniteClient
                    .theme()
                    .colors.magentaAccentColor
            }

            else -> {
                org.infinite.InfiniteClient
                    .theme()
                    .colors.foregroundColor
            }
        }

    fun rarityColor(entity: ItemEntity): Int = rarityColor(entity.item)

    private fun itemBox(
        entity: ItemEntity,
        tickProgress: Float,
    ): AABB {
        // When an entity is removed, it stops moving and its lastRenderX/Y/Z
        // values are no longer updated.
        if (entity.isRemoved) return entity.boundingBox
        val offset: Vec3 =
            itemPos(
                entity,
                tickProgress,
            ).subtract(entity.position())
        return entity.boundingBox.move(offset)
    }

    private fun itemPos(
        entity: ItemEntity,
        partialTicks: Float,
    ): Vec3 {
        if (entity.isRemoved) return entity.position()

        val x: Double = Mth.lerp(partialTicks.toDouble(), entity.xOld, entity.x)
        val y: Double = Mth.lerp(partialTicks.toDouble(), entity.yOld, entity.y)
        val z: Double = Mth.lerp(partialTicks.toDouble(), entity.zOld, entity.z)
        return Vec3(x, y, z)
    }

    fun handleRenderState(
        entity: ItemEntity,
        state: ItemEntityRenderState,
        tickProgress: Float,
        ci: CallbackInfo,
    ) {
        state.outlineColor = rarityColor(entity)
    }
}
