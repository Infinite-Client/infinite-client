package org.infinite.libs.client.player

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.food.FoodConstants
import net.minecraft.world.item.enchantment.Enchantments
import org.infinite.libs.client.inventory.InventoryManager
import org.infinite.libs.server.mod.appleskin.ExhaustionSyncPayload
import org.infinite.utils.item.enchantLevel
import kotlin.math.roundToInt
import kotlin.math.sqrt

// 統計情報の取得ロジックをカプセル化
object PlayerStatsManager : ClientInterface() {
    // プレイヤーの生の統計情報を保持するデータクラス
    data class PlayerStats(
        val hpProgress: Double,
        val maxHp: Float,
        val absorptionProgress: Double,
        val armorProgress: Double,
        val toughnessProgress: Double,
        val hungerLevel: Int,
        val saturationLevel: Float,
        val hungerProgress: Double,
        val saturationProgress: Double,
        val airProgress: Double,
        val vehicleHealthProgress: Double,
        val isJumping: Boolean,
        val totalFoodProgress: Double, // 回復予測用の食料進捗
        val canNaturallyRegenerate: Boolean, // 自然回復フラグ
        val experienceProgress: Double, // 経験値の進捗 (0.0 - 1.0)
        val experienceLevel: Int, // 経験値レベル
    )

    private const val MAX_EXHAUSTION = FoodConstants.EXHAUSTION_DROP.toDouble()
    var exhaustion = 0.0
    private var stats: PlayerStats? = null

    fun tick() {
        val player = player ?: return
        val newStats = updateStats(player)
        manageHunger(player, newStats)
        stats = newStats
    }

    private fun manageHunger(
        player: LocalPlayer,
        newStats: PlayerStats,
    ) {
        val stats = stats ?: return
        val isSprinting = player.isSprinting && player.onGround()
        val isInWater = player.isInWater
        val isJumped = player.isJumping && !stats.isJumping
        val isDamaged = newStats.hpProgress < stats.hpProgress
        val moveLength =
            run {
                val posDiff = player.deltaMovement
                val x = posDiff.x
                val z = posDiff.z
                sqrt(x * x + z * z)
            }
        if (isSprinting) {
            val amplifier = 0.1
            exhaustion += amplifier * moveLength
        }
        if (isInWater) {
            val amplifier = 0.01
            exhaustion += amplifier * moveLength
        }
        if (isJumped) {
            exhaustion += if (isSprinting) 0.2 else 0.05
        }
        val hungerEffect = player.getEffect(MobEffects.HUNGER)
        if (hungerEffect != null) {
            val level = hungerEffect.amplifier
            exhaustion += 0.02 * level
        }
        if (isDamaged) {
            exhaustion += 0.3
        }
        val reducedFoodLevels =
            newStats.hungerLevel - stats.hungerLevel + newStats.saturationLevel - stats.saturationLevel
        if (reducedFoodLevels < 0) {
            exhaustion += reducedFoodLevels * MAX_EXHAUSTION
            return
        }
        exhaustion = exhaustion.coerceIn(0.0, MAX_EXHAUSTION * 10.0)
    }

    fun stats(): PlayerStats? = stats

    private fun updateStats(player: LocalPlayer): PlayerStats {
        // 体力と吸収
        val hp = player.health
        val maxHp = player.maxHealth
        val hpProgress: Double = (hp / maxHp).toDouble()
        // 吸収量を取得。最大吸収量は20 (ハート10個分)
        val absorptionProgress = (player.absorptionAmount / maxHp).coerceAtMost(1f).toDouble()
        // 装甲 (Armor) と装甲強度 (Toughness)
        val armorValue = player.armorValue.toDouble()
        val maxArmor = 20.0
        val armorProgress: Double = armorValue / maxArmor
        val toughnessValue = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
        val maxToughness = 3.0 * 4 // 一般的な最大値
        val toughnessProgress = toughnessValue / maxToughness

        // 満腹度 (Hunger) と 隠し満腹度 (Saturation)
        val hungerManager = player.foodData
        val hungerLevel = hungerManager.foodLevel.toDouble()
        val saturationLevel = hungerManager.saturationLevel.toDouble()
        val maxHunger = FoodConstants.MAX_FOOD.toDouble()
        val exhaustionProgress = exhaustion / MAX_EXHAUSTION
        val saturationProgress =
            if (exhaustionProgress > saturationLevel) {
                0.0
            } else {
                (saturationLevel - exhaustionProgress) / maxHunger
            }
        val hungerProgress: Double =
            if (exhaustionProgress > saturationLevel) {
                (hungerLevel - exhaustionProgress + saturationLevel) / maxHunger
            } else {
                hungerLevel / maxHunger
            }

        // ★ 回復予測に必要な新しい計算
        val foodPoints: Double = hungerLevel + saturationLevel
        val maxFoodPoints: Double = 20.0 + 20.0
        val totalFoodProgress: Double = foodPoints / maxFoodPoints.coerceAtLeast(1.0)
        val canNaturallyRegenerate: Boolean = hungerManager.foodLevel >= 18

        // 空気 (Air)
        val air = player.airSupply.toDouble()
        val maxAir = player.maxAirSupply.toDouble()
        val airProgress = air / maxAir

        // 乗り物の体力 (Vehicle Health)
        val vehicleHealthProgress =
            player.vehicle?.let { vehicle ->
                if (vehicle is LivingEntity) {
                    (vehicle.health / vehicle.maxHealth).toDouble()
                } else {
                    0.0
                }
            } ?: 0.0
        return PlayerStats(
            hpProgress = hpProgress,
            maxHp = maxHp,
            absorptionProgress = absorptionProgress,
            armorProgress = armorProgress,
            toughnessProgress = toughnessProgress,
            hungerProgress = hungerProgress,
            hungerLevel = hungerLevel.roundToInt(),
            saturationLevel = saturationLevel.toFloat(),
            saturationProgress = saturationProgress,
            airProgress = airProgress,
            vehicleHealthProgress = vehicleHealthProgress,
            isJumping = player.isJumping,
            totalFoodProgress = totalFoodProgress.coerceIn(0.0, 1.0),
            canNaturallyRegenerate = canNaturallyRegenerate,
            experienceProgress = player.experienceProgress.toDouble(),
            experienceLevel = player.experienceLevel,
        )
    }

    /**
     * 装備している食物アイテムから得られる栄養と飽和度の情報を取得します。
     * @return Pair<栄養度の進捗, 飽和度の進捗>
     */
    fun foodSaturation(player: LocalPlayer): Pair<Double, Double> {
        val mainHandItem = player.mainHandItem
        val offHandItem = player.offhandItem
        val food =
            mainHandItem.get(DataComponents.FOOD) ?: offHandItem.get(DataComponents.FOOD) ?: return 0.0 to 0.0
        val maxHunger = 20.0
        val nutrition = food.nutrition / maxHunger
        val saturation = food.saturation / maxHunger
        return nutrition to saturation
    }

    /**
     * 残り潜水時間を秒単位で計算します。
     */
    fun diveSeconds(player: LocalPlayer): Int {
        val remainAirTick = player.airSupply
        val airMultiply =
            1 +
                run {
                    val helmetItem = InventoryManager.get(InventoryManager.InventoryIndex.Armor.Head())
                    val level = enchantLevel(helmetItem, Enchantments.RESPIRATION)
                    return@run level
                }
        val remainBreath = player.getEffect(MobEffects.WATER_BREATHING)?.duration ?: 0
        return (remainBreath + remainAirTick * airMultiply) / 20
    }

    fun sprintMeters(player: LocalPlayer): Int {
        val hungerManager = player.foodData
        val maxExhaustion = MAX_EXHAUSTION
        val food = hungerManager.foodLevel
        val saturation = hungerManager.saturationLevel
        val totalLevel = food + saturation - exhaustion / maxExhaustion
        val runnableLevel = totalLevel - 6
        if (runnableLevel <= 0) return 0
        val exhaustionPerMeter = 0.1
        return (runnableLevel * maxExhaustion / exhaustionPerMeter).roundToInt()
    }

    fun swimMeters(player: LocalPlayer): Int {
        val hungerManager = player.foodData
        val maxExhaustion = MAX_EXHAUSTION
        val food = hungerManager.foodLevel
        val saturation = hungerManager.saturationLevel
        val totalLevel = food + saturation - exhaustion / maxExhaustion
        val runnableLevel = totalLevel - 6
        if (runnableLevel <= 0) return 0
        val exhaustionPerMeter = 0.01
        return (runnableLevel * maxExhaustion / exhaustionPerMeter).roundToInt()
    }

    fun handleEntityAttack() {
        exhaustion += 0.1
    }

    fun handleBlockBreak() {
        exhaustion += 0.005
    }

    fun resetHunger() {
        exhaustion = 0.0
    }

    /**
     * AppleSkinの疲労度同期パケットを受信し、exhaustionの値を更新します。
     * @param buf 疲労度のfloat値のみを含むPacketByteBuf
     */
    fun handleAppleSkinExhaustion(buf: ExhaustionSyncPayload) {
        exhaustion = buf.exhaustion().toDouble()
    }

    fun init() {
        ClientTickEvents.START_CLIENT_TICK.register { _ -> tick() }
        ServerPlayerEvents.AFTER_RESPAWN.register { _, _, _ ->
            resetHunger()
        }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            registerReceiver()
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> unregisterReceiver() }
    }

    fun unregisterReceiver() {
        ClientPlayNetworking.unregisterReceiver(ExhaustionSyncPayload.ID.id)
    }

    fun registerReceiver() {
        ClientPlayNetworking.registerReceiver<ExhaustionSyncPayload>(
            ExhaustionSyncPayload.ID,
        ) { payload: ExhaustionSyncPayload, _: ClientPlayNetworking.Context ->
            handleAppleSkinExhaustion(payload)
        }
    }
}
