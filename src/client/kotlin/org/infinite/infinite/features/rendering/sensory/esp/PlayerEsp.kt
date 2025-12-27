package org.infinite.infinite.features.rendering.sensory.esp

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.infinite.infinite.features.rendering.sensory.ExtraSensory
import org.infinite.libs.graphics.Graphics3D // Graphics3D をインポート
import org.infinite.libs.graphics.render.RenderUtils
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.sqrt

object PlayerEsp {
    private val BOX_COLOR =
        org.infinite.InfiniteClient
            .theme()
            .colors.aquaAccentColor
    private const val EXPAND = 0.05

    private fun otherPlayers(): List<Player> {
        val client = Minecraft.getInstance()
        val world = client.level
        val self = client.player

        return world
            ?.entitiesForRendering()
            ?.filter {
                // PlayerEntity かつ 自分自身ではない
                it is Player && it != self && it.isAlive
            }?.map {
                it as Player
            }
            ?: return emptyList()
    }

    /**
     * Graphics3D を利用して他のプレイヤーエンティティのアウトラインとコネクションラインを描画します。
     *
     * @param graphics3d 描画コンテキスト
     */
    fun render(
        graphics3d: Graphics3D,
        value: ExtraSensory.Method, // Graphics3D を引数として受け取る
    ) {
        if (value == ExtraSensory.Method.OutLine) return
        val client = Minecraft.getInstance()

        // Graphics3D から tickProgress を取得
        val tickProgress = graphics3d.tickProgress

        val players = otherPlayers()

        // 1. プレイヤーの枠線 (水色) を描画
        val renderBoxes =
            players.map { player ->
                RenderUtils.ColorBox(
                    BOX_COLOR,
                    playerBox(player, tickProgress).inflate(EXPAND),
                )
            }
        // Graphics3D のメソッドを呼び出す
        graphics3d.renderLinedColorBoxes(renderBoxes, true)

        // 2. 自分とプレイヤーを結ぶ直線を描画
        val selfPos = client.player?.getPosition(graphics3d.tickCounter.getGameTimeDeltaPartialTick(true)) ?: return
        for (player in players) {
            val playerPos = playerPos(player, tickProgress)
            // プレイヤーの足元ではなく、目の高さ（中間点）を使用
            val playerLineTarget = playerPos.add(0.0, player.bbHeight / 2.0, 0.0)

            // 距離を計算 (X, Y, Zの距離)
            val dx = selfPos.x - playerLineTarget.x
            val dy = selfPos.y - playerLineTarget.y
            val dz = selfPos.z - playerLineTarget.z
            val distance = sqrt(dx * dx + dy * dy + dz * dz)

            // 距離に基づいて色を決定
            val lineColor = RenderUtils.distColor(distance)
            // Graphics3D のメソッドを利用して直線を描画
            graphics3d.renderLine(playerLineTarget, selfPos, lineColor, true)
        }
    }

    /**
     * プレイヤーの現在の描画位置に基づいてBoxを取得する
     */
    private fun playerBox(
        entity: Player,
        tickProgress: Float,
    ): AABB {
        if (entity.isRemoved) return entity.boundingBox
        val offset: Vec3 =
            playerPos(
                entity,
                tickProgress,
            ).subtract(entity.position())
        return entity.boundingBox.move(offset)
    }

    /**
     * tickProgress (partialTicks) を使用して、プレイヤーの補間された位置を計算する
     */
    private fun playerPos(
        entity: Player,
        partialTicks: Float,
    ): Vec3 {
        if (entity.isRemoved) return entity.position()

        val x: Double = Mth.lerp(partialTicks.toDouble(), entity.xOld, entity.x)
        val y: Double = Mth.lerp(partialTicks.toDouble(), entity.yOld, entity.y)
        val z: Double = Mth.lerp(partialTicks.toDouble(), entity.zOld, entity.z)
        return Vec3(x, y, z)
    }

    fun handleRenderState(
        entity: Player,
        state: AvatarRenderState,
        tickProgress: Float,
        ci: CallbackInfo,
    ) {
        state.outlineColor = BOX_COLOR
    }
}
