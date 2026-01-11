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

    private var isAiming = false

    override fun onEnabled() {
        findAndLockTarget()
    }

    override fun onDisabled() {
        lockedEntity = null
        isAiming = false
    }

    override fun onStartTick() {
        if (!isEnabled()) return

        val p = player ?: return
        val currentTarget = lockedEntity

        // --- 1. ターゲットの有効性・距離チェック ---
        if (currentTarget != null) {
            val distance = p.distanceTo(currentTarget)
            // ターゲットが死亡している、または設定距離より「1ブロック以上」離れたら解除
            if (!currentTarget.isAlive || distance > (range.value + 1.0)) {
                lockedEntity = null
                isAiming = false
                // 自動解除された後、次のターゲットを即座に探さない（見逃し防止）
                return
            }
        }

        // --- 2. ターゲットがいない場合のみ検索 ---
        if (lockedEntity == null) {
            findAndLockTarget()
        }

        // --- 3. エイムタスクの発行 ---
        // ターゲットがいて、かつ現在エイム中でない場合のみタスクを投げる
        if (lockedEntity != null && !isAiming) {
            isAiming = true
            AimSystem.addTask(createLockOnTask(lockedEntity!!))
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
                    // Featureが無効、またはターゲットが変更/解除されたら中断
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
                // エイム完了。フラグを落として次のTickで必要なら再エイム（追従）
                isAiming = false
            },
            onFailure = {
                isAiming = false
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
