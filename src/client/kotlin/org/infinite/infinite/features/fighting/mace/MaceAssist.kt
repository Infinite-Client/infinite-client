package org.infinite.infinite.features.fighting.mace

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.infinite.InfiniteClient
import org.infinite.libs.client.aim.AimInterface
import org.infinite.libs.client.aim.task.AimTask
import org.infinite.libs.client.aim.task.condition.AimTaskConditionInterface
import org.infinite.libs.client.aim.task.condition.AimTaskConditionReturn
import org.infinite.libs.client.aim.task.config.AimCalculateMethod
import org.infinite.libs.client.aim.task.config.AimPriority
import org.infinite.libs.client.aim.task.config.AimTarget
import org.infinite.libs.client.inventory.InventoryManager
import org.infinite.libs.client.player.ClientInterface
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.libs.graphics.Graphics3D
import org.infinite.settings.FeatureSetting
import org.infinite.utils.item.enchantLevel
import org.infinite.utils.rendering.Line
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class MaceAssist : ConfigurableFeature() {
    private val searchTickSetting = FeatureSetting.IntSetting("SearchTick", 128, 0, 256)
    private val reactTickSetting = FeatureSetting.IntSetting("ReactTick", 10, 0, 10)
    private val methodSetting =
        FeatureSetting.EnumSetting("Method", AimCalculateMethod.Linear, AimCalculateMethod.entries)
    override val settings: List<FeatureSetting<*>> = listOf(searchTickSetting, reactTickSetting, methodSetting)
    private var targetEntities: List<LivingEntity> = emptyList()
    private var fallDistance = 0.0
    private var isCollision = false
    private var remainTick = 0
    private var calculatedPos: Vec3 = Vec3.ZERO

    // 💡 追加: この落下中に攻撃が実行されたかを追跡するフラグ
    private var hasAttackedInFall = false
    private val haveMace: Boolean
        get() =
            InventoryManager.get(InventoryManager.InventoryIndex.MainHand()).item == Items.MACE

    override fun onTick() {
        calcFallDistance()
        if (!haveMace) return
        // ターゲットエンティティの検索ロジック（変更なし）
        targetEntities =
            if (isCollision) {
                val reactTick = reactTickSetting.value
                if (remainTick <= reactTick) {
                    searchTargetEntities(calculatedPos)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }

        // 💡 追加: 攻撃の予約/実行ロジック
        val reactTick = reactTickSetting.value

        // 衝突が予測され、残りティック数が範囲内で、ターゲットがいて、まだ攻撃していない場合
        if (isCollision && remainTick <= reactTick && targetEntities.isNotEmpty() && !hasAttackedInFall) {
            // 最適なターゲットを選択（ここではリストの最初のエンティティ）
            val target = targetEntities.first()
            // 攻撃関数を実行
            attackEntity(target)
            hasAttackedInFall = true
        }
    }

    private fun updateCalculatedPositions(progress: Float) {
        val (a, b, c) = calcFallPosition(searchTickSetting.value, progress) ?: return
        calculatedPos = a
        isCollision = b
        remainTick = c
    }

    override fun render3d(graphics3D: Graphics3D) {
        updateCalculatedPositions(graphics3D.tickProgress)
        val player = player ?: return
        if (haveMace && !hasAttackedInFall && remainTick >= reactTickSetting.value && !player.onGround()) {
            val radius = player.entityInteractionRange()
            val pos = calculatedPos
            val centerX = pos.x
            val centerY = pos.y
            val centerZ = pos.z
            val color = InfiniteClient.theme().colors.primaryColor
            graphics3D.pushMatrix()
            graphics3D.translate(centerX, centerY, centerZ)
            renderCircle(graphics3D, radius, color)
            graphics3D.popMatrix()
        }
    }

    /**
     * Y軸に垂直な円を描画するヘルパー関数
     * @param graphics3D 描画コンテキスト
     * @param radius 半径
     * @param color 色
     */
    private fun renderCircle(
        graphics3D: Graphics3D,
        radius: Double,
        color: Int,
    ) {
        val segments = max((radius * 2).roundToInt(), 16)
        val lines = mutableListOf<Line>()
        for (i in 0 until segments) {
            val angle1 = (i.toDouble() / segments.toDouble()) * 2 * PI
            val angle2 = ((i + 1).toDouble() / segments.toDouble()) * 2 * PI
            // 円周上の2点 (X-Z平面)
            val x1 = radius * cos(angle1)
            val z1 = radius * sin(angle1)
            val x2 = radius * cos(angle2)
            val z2 = radius * sin(angle2)
            // Y座標は0 (translateで既に centerY に移動しているため)
            val start = Vec3(x1, 0.0, z1)
            val end = Vec3(x2, 0.0, z2)
            lines.add(Line(start, end, color))
        }
        // 描画
        graphics3D.renderLinedLines(lines, true)
    }

    /**
     * 衝突予測地点の周囲にいるターゲットエンティティを検索する。
     * * @param smashPos 予測された衝突地点 (落下地点のPos)
     * @return ターゲット条件を満たす LivingEntity のリスト
     */
    private fun searchTargetEntities(smashPos: Vec3): List<LivingEntity> {
        val player = player ?: return emptyList()
        val world = world ?: return emptyList()
        // メイスのスマッシュ攻撃のターゲット検索範囲（例: 5ブロック半径）
        val searchRadius = player.entityInteractionRange()
        // 衝突予測地点を中心とした検索範囲を定義
        val searchBox =
            AABB(
                smashPos.x - searchRadius,
                smashPos.y - searchRadius,
                smashPos.z - searchRadius,
                smashPos.x + searchRadius,
                smashPos.y + searchRadius,
                smashPos.z + searchRadius,
            )
        // 指定された範囲内のすべての LivingEntity を検索し、フィルタリングする
        return world
            .getEntities(player, searchBox)
            .filter { entity ->
                entity != player && entity is LivingEntity &&
                    entity.isAlive
            }.map { it as LivingEntity }
            .toList()
    }

    private fun calcFallDistance() {
        val velocity = player?.deltaMovement ?: return
        val y = velocity.y
        if (player?.onGround() == true) {
            hasAttackedInFall = false
        }
        if (y > 0) {
            hasAttackedInFall = false // 攻撃フラグをリセットし、次の落下に備える
            fallDistance = 0.0
        } else {
            fallDistance -= y
        }
    }

    private class MaceAttackCondition(
        val target: LivingEntity,
    ) : ClientInterface(),
        AimTaskConditionInterface {
        override fun check(): AimTaskConditionReturn {
            val player = player ?: return AimTaskConditionReturn.Failure
            if (player.fallDistance == 0.0) return AimTaskConditionReturn.Failure
            return if (player.distanceTo(target) < player.entityInteractionRange()) {
                interactionManager?.attack(player, target)
                AimTaskConditionReturn.Force
            } else {
                AimTaskConditionReturn.Exec
            }
        }
    }

    // 💡 追加: 攻撃を実行するスタブ関数
    private fun attackEntity(target: LivingEntity) {
        AimInterface.addTask(
            AimTask(
                AimPriority.Preferentially,
                AimTarget.EntityTarget(target),
                MaceAttackCondition(target),
                methodSetting.value,
            ),
        )
    }

    private fun calcFallPosition(
        ticks: Int,
        progress: Float,
    ): Triple<Vec3, Boolean, Int>? {
        if (vehicle != null) return null
        val player = player ?: return null
        val world = world ?: return null
        var velocity = player.deltaMovement
        val gravity = player.gravity // 重力加速度
        val friction = 0.98 // エンティティの速度に適用される摩擦
        var pos = player.getPosition(progress) // プレイヤーの現在位置 (Posクラスを想定)
        var isCollision = false
        var remainingTick = ticks
        for (i in 0 until ticks) {
            val nextPos = pos.add(velocity)
            // 注意: playerPos!! はコード内に定義されていません。
            // プレイヤーの現在の位置 (player.posなど) を想定していると思われます。
            isCollision =
                world
                    .getBlockAndLiquidCollisions(player, player.boundingBox.move(nextPos.subtract(playerPos!!)))
                    .toList()
                    .isNotEmpty()
            if (isCollision) break
            pos = nextPos
            velocity = velocity.scale(friction)
            velocity = velocity.subtract(0.0, gravity, 0.0)
            remainingTick = i
        }
        return Triple(pos, isCollision, remainingTick)
    }

    /**
     * メイスで与えられるダメージを計算する
     * @param fallDistance 落下距離
     * @return ダメージ量
     */
    fun maceDamage(): Double? {
        val stack = InventoryManager.get(InventoryManager.InventoryIndex.MainHand())
        if (stack.item != Items.MACE) return null
        val densityLevel = enchantLevel(stack, Enchantments.DENSITY)
        val normalDamage = 6.0
        val criticalBaseDamage = 9.0
        val criticalPoint = 1.0
        val smashPoint = 1.5

        fun smashDamage(distance: Double): Double {
            val damageCalcInfo =
                listOf(
                    4 + densityLevel / 2.0 to 1.5..3.0,
                    2 + densityLevel / 2.0 to 3.0..8.0,
                    2 + densityLevel / 2.0 to 8.0..Double.POSITIVE_INFINITY,
                )
            var damage = 0.0
            for ((multiply, range) in damageCalcInfo) {
                if (distance in range) {
                    damage += multiply * (distance - range.start)
                    break
                } else if (distance > range.endInclusive) {
                    damage += multiply * (range.endInclusive - range.start)
                }
            }
            return damage
        }
        return when {
            fallDistance < criticalPoint -> normalDamage
            fallDistance > criticalPoint && fallDistance < smashPoint -> criticalBaseDamage
            fallDistance > smashPoint -> criticalBaseDamage + smashDamage(fallDistance)
            else -> null
        }
    }
}
