package org.infinite.infinite.features.rendering.sensory.esp

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.util.Mth
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.infinite.infinite.features.rendering.sensory.ExtraSensory
import org.infinite.libs.graphics.Graphics3D // Graphics3D をインポート
import org.infinite.libs.graphics.render.RenderUtils
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object MobEsp {
    private fun livingEntities(): List<LivingEntity> {
        // 現在のワールドに存在する、プレイヤー自身ではないLivingEntityのリストを取得
        val client = Minecraft.getInstance()
        val world = client.level

        return world
            ?.entitiesForRendering()
            ?.filter {
                // LivingEntity かつ プレイヤー自身ではない
                it is LivingEntity && it !is Player
            }?.map {
                it as LivingEntity
            }
            ?: return emptyList()
    }

    /**
     * Graphics3D を利用してMobエンティティのアウトラインを描画します。
     *
     * @param graphics3d 描画コンテキスト
     */
    fun render(
        graphics3d: Graphics3D,
        value: ExtraSensory.Method, // Graphics3D を引数として受け取る
    ) {
        if (value == ExtraSensory.Method.OutLine) return
        // Graphics3D から tickProgress を取得
        val tickProgress = graphics3d.tickProgress
        val expand = 0.05 // 描画するBoxをわずかに拡張

        val mobs = livingEntities()
        val renderBoxes =
            mobs.map {
                RenderUtils.ColorBox(
                    mobColor(it),
                    mobBox(it, tickProgress)
                        .inflate(expand), // Boxを拡張
                )
            }

        // Graphics3D のラッパーメソッドを利用し、MatrixStack の管理とフラッシュを Graphics3D に任せる
        graphics3d.renderLinedColorBoxes(renderBoxes, true)
    }

    /**
     * モブの種別に基づいて色を決定する
     */
    private fun mobColor(entity: LivingEntity): Int = when (entity) {
        is Monster -> {
            org.infinite.InfiniteClient
                .theme()
                .colors.redAccentColor
        }

        // 敵対モブ -> 赤
        is AgeableMob -> {
            org.infinite.InfiniteClient
                .theme()
                .colors.greenAccentColor
        }

        // 友好モブ -> 緑
        // 中立モブ、あるいはどちらにも分類されないモブ -> 黄
        else -> {
            org.infinite.InfiniteClient
                .theme()
                .colors.yellowAccentColor
        }
    }

    /**
     * モブの現在の描画位置に基づいてBoxを取得する
     */
    private fun mobBox(
        entity: LivingEntity,
        tickProgress: Float,
    ): AABB {
        if (entity.isRemoved) return entity.boundingBox

        val offset: Vec3 =
            mobPos(
                entity,
                tickProgress,
            ).subtract(entity.position())
        return entity.boundingBox.move(offset)
    }

    /**
     * tickProgress (partialTicks) を使用して、モブの補間された位置を計算する
     */
    private fun mobPos(
        entity: LivingEntity,
        partialTicks: Float,
    ): Vec3 {
        if (entity.isRemoved) return entity.position()

        // MathHelper.lerp を使用して位置を補間
        val x: Double = Mth.lerp(partialTicks.toDouble(), entity.xOld, entity.x)
        val y: Double = Mth.lerp(partialTicks.toDouble(), entity.yOld, entity.y)
        val z: Double = Mth.lerp(partialTicks.toDouble(), entity.zOld, entity.z)
        return Vec3(x, y, z)
    }

    fun handleRenderState(
        entity: Mob,
        state: LivingEntityRenderState,
        tickProgress: Float,
        ci: CallbackInfo,
    ) {
        state.outlineColor = mobColor(entity)
    }
}
