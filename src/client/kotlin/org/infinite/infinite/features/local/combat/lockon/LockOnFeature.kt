package org.infinite.infinite.features.local.combat.lockon

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.minecraft.aim.AimSystem
import org.infinite.libs.minecraft.aim.task.AimTask
import org.infinite.libs.minecraft.aim.task.condition.AimTaskConditionInterface
import org.infinite.libs.minecraft.aim.task.condition.AimTaskConditionReturn
import org.infinite.libs.minecraft.aim.task.config.AimCalculateMethod
import org.infinite.libs.minecraft.aim.task.config.AimPriority
import org.infinite.libs.minecraft.aim.task.config.AimTarget
import org.infinite.utils.isLookingAtEntity
import org.lwjgl.glfw.GLFW
import kotlin.math.PI
import kotlin.math.acos

class LockOnFeature : LocalFeature() {
    override val defaultToggleKey: Int = GLFW.GLFW_KEY_K

    // 現在ロックしているエンティティをタスクから逆引きする（読み取り専用）
    val lockedEntity: Entity?
        get() = (currentTask?.target as? AimTarget.EntityTarget)?.entity

    // セッターを通じて AimSystem 内のタスクを常に1つに保つ
    private var currentTask: AimTask? = null
        set(value) {
            field?.let { AimSystem.remove(it) }
            value?.let { AimSystem.addTask(it) }
            field = value
        }

    override fun onEnabled() {
        findAndLockTarget()
    }

    override fun onDisabled() {
        currentTask = null // タスクを破棄してエイム停止
    }

    private val range by property(DoubleProperty(16.0, 3.0, 256.0))
    private val players by property(BooleanProperty(true))
    private val mobs by property(BooleanProperty(true))
    private val fov by property(DoubleProperty(90.0, 10.0, 360.0))
    private val aimSpeed by property(DoubleProperty(1.0, 0.5, 10.0))
    private val method by property(EnumSelectionProperty(AimCalculateMethod.Linear))
    private val priorityMode by property(EnumSelectionProperty(Priority.Both))
    private val anchorPoint by property(EnumSelectionProperty(AimTarget.EntityTarget.EntityAnchor.Center))
    private val autoAttack by property(BooleanProperty(false))

    // 追加: 射線が通っているか確認するオプション
    private val checkLineOfSight by property(BooleanProperty(true))

    enum class Priority { Direction, Distance, Both }

    override fun onStartTick() {
        if (!isEnabled()) return

        val player = this@LockOnFeature.player ?: run {
            disable()
            return
        }

        val target = lockedEntity
        if (target != null) {
            val distance = player.distanceTo(target)

            // ターゲットが有効かどうかの判定に射線チェックを追加
            val isDead = !target.isAlive
            val isOutOfRange = distance > (range.value + 1.5)
            val isOccluded = checkLineOfSight.value && !player.hasLineOfSight(target)

            if (isDead || isOutOfRange || isOccluded) {
                disable()
                return
            }

            // 自動攻撃
            if (autoAttack.value) {
                if (player.getAttackStrengthScale(0f) >= 0.9f && player.isLookingAtEntity(target)) {
                    minecraft.gameMode?.attack(player, target)
                    player.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
                }
            }
        } else {
            // 索敵を継続
            findAndLockTarget()
        }
    }

    private fun findAndLockTarget() {
        val player = this@LockOnFeature.player ?: return
        val level = this@LockOnFeature.level ?: return

        val candidates = level.getEntities(player, player.boundingBox.inflate(range.value))
            .asSequence()
            .filterIsInstance<LivingEntity>()
            .filter { it != player && it.isAlive }
            .filter { (players.value && it is Player) || (mobs.value && it !is Player) }
            .filter { player.distanceTo(it) <= range.value }
            .filter { isWithinFOV(player, it, fov.value) }
            // 射線チェックオプションを適用
            .filter { !checkLineOfSight.value || player.hasLineOfSight(it) }
            .toList()

        val bestTarget = when (priorityMode.value) {
            Priority.Direction -> candidates.minByOrNull { getAngle(player, it) }
            Priority.Distance -> candidates.minByOrNull { player.distanceTo(it) }
            Priority.Both -> candidates.minByOrNull { calculateCombinedScore(player, it) }
        }

        currentTask = bestTarget?.let { createLockOnTask(it) }
    }

    private fun createLockOnTask(target: LivingEntity): AimTask {
        return AimTask(
            priority = AimPriority.Normally,
            target = AimTarget.EntityTarget(target, anchorPoint.value),
            condition = object : AimTaskConditionInterface {
                override fun check(): AimTaskConditionReturn {
                    // 機能が無効化されたか、ターゲットがすり替わっていないか確認
                    return if (isEnabled() && lockedEntity == target) {
                        AimTaskConditionReturn.Exec
                    } else {
                        AimTaskConditionReturn.Failure
                    }
                }
            },
            calcMethod = method.value,
            multiply = aimSpeed.value,
            onSuccess = {
                // Success/Failure時、このタスクを管理対象から外す
                if (currentTask?.target == AimTarget.EntityTarget(target, anchorPoint.value)) {
                    currentTask = null
                }
            },
            onFailure = {
                if (currentTask?.target == AimTarget.EntityTarget(target, anchorPoint.value)) {
                    currentTask = null
                }
            },
        )
    }

    private fun getAngle(p: Player, target: LivingEntity): Double {
        val playerLookVec = p.lookAngle.normalize()
        val targetVec = target.boundingBox.center.subtract(p.eyePosition).normalize()
        val dotProduct = playerLookVec.dot(targetVec)
        return Math.toDegrees(acos(dotProduct.coerceIn(-1.0, 1.0)))
    }

    private fun isWithinFOV(p: Player, target: LivingEntity, fovDegrees: Double): Boolean {
        return getAngle(p, target) <= fovDegrees / 2.0
    }

    private fun calculateCombinedScore(p: Player, target: LivingEntity): Double {
        val distNormalized = (p.distanceTo(target) / range.value).coerceIn(0.0, 1.0)
        val angleNormalized = (getAngle(p, target) / (fov.value / 2.0)).coerceIn(0.0, 1.0)
        return (angleNormalized * 0.6) + (distNormalized * 0.4)
    }

    override fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D {
        val target = lockedEntity ?: return graphics2D
        val targetPos = target.getPosition(graphics2D.gameDelta).add(0.0, target.eyeHeight.toDouble(), 0.0)
        val screenPos = graphics2D.projectWorldToScreen(targetPos) ?: return graphics2D

        val x = screenPos.first.toFloat()
        val y = screenPos.second.toFloat()
        val size = 12f
        val color = InfiniteClient.theme.colorScheme.accentColor

        graphics2D.beginPath()
        graphics2D.strokeStyle.color = color
        graphics2D.strokeStyle.width = 2f

        // 円形のターゲットマーク
        graphics2D.arc(x, y, size * 0.75f, 0f, (PI * 2).toFloat())
        graphics2D.strokePath()

        // X字のレティクル
        graphics2D.beginPath()
        graphics2D.moveTo(x - size, y - size)
        graphics2D.lineTo(x + size, y + size)
        graphics2D.strokePath()
        graphics2D.beginPath()
        graphics2D.moveTo(x + size, y - size)
        graphics2D.lineTo(x - size, y + size)
        graphics2D.strokePath()

        return graphics2D
    }
}
