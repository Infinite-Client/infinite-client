package org.infinite.infinite.features.local.combat.lockon

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
import org.lwjgl.glfw.GLFW
import kotlin.math.PI
import kotlin.math.acos

class LockOnFeature : LocalFeature() {
    override val defaultToggleKey: Int = GLFW.GLFW_KEY_K

    // --- プロパティ定義 ---
    private val range by property(DoubleProperty(16.0, 3.0, 64.0))
    private val players by property(BooleanProperty(true))
    private val mobs by property(BooleanProperty(true))
    private val fov by property(DoubleProperty(90.0, 10.0, 360.0))
    private val aimSpeed by property(DoubleProperty(1.0, 0.5, 10.0))
    private val method by property(EnumSelectionProperty(AimCalculateMethod.Linear))
    private val priorityMode by property(EnumSelectionProperty(Priority.Both))
    private val anchorPoint by property(EnumSelectionProperty(AimTarget.EntityTarget.EntityAnchor.Center))

    enum class Priority { Direction, Distance, Both }

    var lockedEntity: LivingEntity? = null
        private set

    override fun onEnabled() {
        findAndLockTarget()
    }

    override fun onDisabled() {
        lockedEntity = null
    }

    override fun onStartTick() {
        if (!isEnabled()) return

        val p = player ?: return
        val target = lockedEntity

        // ターゲットの有効性チェック
        if (target == null || !target.isAlive || p.distanceTo(target) > range.value) {
            lockedEntity = null
            // ターゲットをロストした場合は再検索、見つからなければ無効化
            findAndLockTarget()
            if (lockedEntity == null) {
                toggle() // または disable()
                return
            }
        }

        // AimSystemにタスクがない場合のみ追加 (継続的な追従)
        if (AimSystem.currentTask() == null) {
            lockedEntity?.let {
                AimSystem.addTask(createLockOnTask(it))
            }
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
            .toList()

        lockedEntity = when (priorityMode.value) {
            Priority.Direction -> candidates.minByOrNull { getAngle(player, it) }
            Priority.Distance -> candidates.minByOrNull { player.distanceTo(it) }
            Priority.Both -> candidates.minByOrNull { calculateCombinedScore(player, it) }
        }
    }

    private fun createLockOnTask(target: LivingEntity): AimTask {
        return AimTask(
            priority = AimPriority.Preferentially,
            target = AimTarget.EntityTarget(target, anchorPoint.value),
            condition = object : AimTaskConditionInterface {
                override fun check(): AimTaskConditionReturn {
                    return if (isEnabled() && lockedEntity == target) {
                        AimTaskConditionReturn.Exec
                    } else {
                        AimTaskConditionReturn.Success
                    }
                }
            },
            calcMethod = method.value,
            multiply = aimSpeed.value,
        )
    }

    // --- 計算ユーティリティ ---

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

    // --- レンダリング ---

    override fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D {
        val target = lockedEntity ?: return graphics2D
        // 目の高さにマークを表示
        val targetPos = target.getPosition(graphics2D.gameDelta).add(0.0, target.eyeHeight.toDouble(), 0.0)
        val screenPos = graphics2D.projectWorldToScreen(targetPos) ?: return graphics2D

        val x = screenPos.first.toFloat()
        val y = screenPos.second.toFloat()
        val size = 10f
        val color = InfiniteClient.theme.colorScheme.accentColor

        graphics2D.beginPath()
        graphics2D.strokeStyle.color = color
        graphics2D.strokeStyle.width = 2f

        // ターゲットを囲む四角形
        graphics2D.beginPath()
        graphics2D.arc(
            x,
            y,
            size * 0.75f,
            0f,
            (PI * 2).toFloat(),
        )
        graphics2D.strokePath()

        // 十字線
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
