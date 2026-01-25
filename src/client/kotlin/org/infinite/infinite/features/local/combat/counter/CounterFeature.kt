package org.infinite.infinite.features.local.combat.counter

import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.minecraft.aim.AimSystem
import org.infinite.libs.minecraft.aim.task.AimTask
import org.infinite.libs.minecraft.aim.task.condition.AimTaskConditionByFrame
import org.infinite.libs.minecraft.aim.task.config.AimCalculateMethod
import org.infinite.libs.minecraft.aim.task.config.AimPriority
import org.infinite.libs.minecraft.aim.task.config.AimTarget
import kotlin.math.PI

class CounterFeature : LocalFeature() {

    // --- プロパティ定義 (Property Delegateを使用) ---
    private val reactionTick by property(IntProperty(4, 0, 10, " ticks"))
    private val processTick by property(IntProperty(4, 0, 10, " ticks"))
    private val randomizer by property(IntProperty(2, 0, 10, " ticks"))
    private val method by property(EnumSelectionProperty(AimCalculateMethod.Linear))
    private val aimSpeed by property(DoubleProperty(5.0, 1.0, 10.0))
    private val anchorPoint by property(EnumSelectionProperty(AimTarget.EntityTarget.EntityAnchor.Chest))

    override val featureType = FeatureLevel.Utils

    override fun onEndTick() {
        val player = player ?: run {
            target = null
            return
        }
        target?.distanceTo(player)?.let {
            if (it > player.entityAttackRange().maxRange) {
                target = null
            }
        }
    }

    /**
     * ダメージパケット受信時の処理
     */
    fun onDamageReceived(packet: ClientboundDamageEventPacket) {
        if (!isEnabled()) return

        val world = level ?: return
        val player = player ?: return

        // 自分自身のダメージか確認
        if (packet.entityId != player.id) return

        // 攻撃者を取得 (DamageSourceの情報を元に)
        val source = packet.getSource(world)
        val attacker = source.entity

        if (attacker is LivingEntity && attacker != player) {
            executeCounterAttack(attacker)
        }
    }

    private var target: Entity? = null
    private fun executeCounterAttack(target: LivingEntity) {
        val current = AimSystem.currentTask()
        if (current?.target is AimTarget.EntityTarget) {
            if ((current.target as AimTarget.EntityTarget).entity.id == target.id) {
                return
            }
        }
        val randOffset = if (randomizer.value > 0) (0..randomizer.value).random() else 0

        val react = reactionTick.value + randOffset
        val progress = processTick.value + randOffset
        val total = react + progress

        // AimSystemにタスクを登録
        this.target = target
        AimSystem.addTask(
            AimTask(
                priority = AimPriority.Preferentially,
                target = AimTarget.EntityTarget(target, anchorPoint.value),
                condition = AimTaskConditionByFrame(react, total, true),
                calcMethod = method.value,
                multiply = aimSpeed.value,
                onSuccess = {
                    this@CounterFeature.target = null
                    val player = this@CounterFeature.player ?: return@AimTask
                    minecraft.gameMode?.attack(player, target)
                    player.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
                },
            ),
        )
    }

    override fun onStartUiRendering(graphics2D: Graphics2D) {
        val target = target?.getPosition(graphics2D.gameDelta) ?: return
        val pos2d = graphics2D.projectWorldToScreen(target) ?: return
        val pos2f = pos2d.first to pos2d.second
        graphics2D.beginPath()
        val size = 12f
        graphics2D.arc(pos2f.first, pos2f.second, size * 0.75f, 0f, (2 * PI).toFloat())
        graphics2D.strokeStyle.width = 2f
        graphics2D.strokeStyle.color = InfiniteClient.theme.colorScheme.accentColor
        graphics2D.strokePath()
        graphics2D.beginPath()
        graphics2D.moveTo(pos2f.first - size, pos2f.second)
        graphics2D.lineTo(pos2f.first + size, pos2f.second)
        graphics2D.strokePath()
        graphics2D.beginPath()
        graphics2D.moveTo(pos2f.first, pos2f.second - size)
        graphics2D.lineTo(pos2f.first, pos2f.second + size)
        graphics2D.strokePath()
    }
}
