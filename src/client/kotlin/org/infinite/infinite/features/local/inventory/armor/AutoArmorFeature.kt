package org.infinite.infinite.features.local.inventory.armor

import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.item.enchantLevel
import org.infinite.libs.minecraft.multiplayer.inventory.InventorySystem
import org.infinite.libs.minecraft.multiplayer.inventory.structs.InventoryIndex
import org.lwjgl.glfw.GLFW

class AutoArmorFeature : LocalFeature() {

    override val defaultToggleKey: Int = GLFW.GLFW_KEY_UNKNOWN

    val autoEquip by property(BooleanProperty(true))
    val elytraSwitch by property(BooleanProperty(true))
    val durabilityThreshold by property(IntProperty(5, 0, 100, "%"))
    val delay by property(IntProperty(2, 1, 20, "ticks"))

    private var tickDelay = 0
    private var isElytraEquippedByFeature = false
    private var previousChestplate: ItemStack = ItemStack.EMPTY
    private var floatTick = 0
    private var wasJumpKeyPressed = false

    private val armorDefenceValues = mapOf(
        Items.LEATHER_HELMET to 1,
        Items.LEATHER_CHESTPLATE to 3,
        Items.LEATHER_LEGGINGS to 2,
        Items.LEATHER_BOOTS to 1,
        Items.CHAINMAIL_HELMET to 2,
        Items.CHAINMAIL_CHESTPLATE to 5,
        Items.CHAINMAIL_LEGGINGS to 4,
        Items.CHAINMAIL_BOOTS to 1,
        Items.IRON_HELMET to 2,
        Items.IRON_CHESTPLATE to 6,
        Items.IRON_LEGGINGS to 5,
        Items.IRON_BOOTS to 2,
        Items.GOLDEN_HELMET to 2,
        Items.GOLDEN_CHESTPLATE to 5,
        Items.GOLDEN_LEGGINGS to 3,
        Items.GOLDEN_BOOTS to 1,
        Items.DIAMOND_HELMET to 3,
        Items.DIAMOND_CHESTPLATE to 8,
        Items.DIAMOND_LEGGINGS to 6,
        Items.DIAMOND_BOOTS to 3,
        Items.NETHERITE_HELMET to 3,
        Items.NETHERITE_CHESTPLATE to 8,
        Items.NETHERITE_LEGGINGS to 6,
        Items.NETHERITE_BOOTS to 3,
    )

    private val armorToughnessValues = mapOf(
        Items.DIAMOND_HELMET to 2,
        Items.DIAMOND_CHESTPLATE to 2,
        Items.DIAMOND_LEGGINGS to 2,
        Items.DIAMOND_BOOTS to 2,
        Items.NETHERITE_HELMET to 3,
        Items.NETHERITE_CHESTPLATE to 3,
        Items.NETHERITE_LEGGINGS to 3,
        Items.NETHERITE_BOOTS to 3,
    )

    override fun onStartTick() {
        val player = player ?: return
        val inv = InventorySystem

        if (minecraft.screen != null || player.isSpectator) return

        if (tickDelay > 0) {
            tickDelay--
            return
        }

        if (elytraSwitch.value && handleElytraSwitch(inv)) return
        if (autoEquip.value && !isElytraEquippedByFeature) {
            if (!player.isInWater) {
                handleAutoEquip(inv)
            }
        }
    }

    private var outOfWaterTick = 0 // 水から出た後のカウント

    private fun handleElytraSwitch(inv: InventorySystem): Boolean {
        val player = player ?: return false
        val currentChest = inv.getItem(InventoryIndex.Armor.Chest)
        val isPressedJump = options.keyJump.isDown
        val isFlying = player.isFallFlying

        // 水中ならカウントを増やす、出たら減らしていく（猶予を作る）
        if (player.isInWater) {
            outOfWaterTick = 10 // 水から出た後 0.5秒間は「脱出中」とみなす
        } else if (outOfWaterTick > 0) {
            outOfWaterTick--
        }

        // 1. エリトラを脱ぐ条件
        if (isElytraEquippedByFeature) {
            // 水中に入ってから少し経ったか、着地した場合のみ脱ぐ
            // (入った瞬間に脱ぐと、水面ジャンプでまた装備してチャタリングするため)
            val shouldUnequip =
                player.onGround() || (player.isInWater && outOfWaterTick < 5) || (!isFlying && floatTick > 20)

            if (shouldUnequip) {
                if (equipBackOriginalChest(inv)) {
                    isElytraEquippedByFeature = false
                    tickDelay = delay.value
                    return true
                }
            }
        }

        floatTick = if (player.onGround() || player.isInWater) 0 else floatTick + 1

        // 2. エリトラを装備する条件
        // 「空中である」かつ「(水中にいない OR 水から出た直後である)」
        val isExitingWater = outOfWaterTick > 0 && !player.onGround()

        val canEquipElytra = !isElytraEquippedByFeature &&
            !player.onGround() &&
            (isExitingWater || !player.isInWater) && // 水中脱出中なら許可
            (floatTick > 5 || isExitingWater) && // 脱出中は即座に許可
            isPressedJump &&
            !wasJumpKeyPressed &&
            currentChest.item != Items.ELYTRA

        if (canEquipElytra) {
            val elytraIdx = findBestElytra(inv)
            if (elytraIdx != null) {
                previousChestplate = currentChest.copy()
                inv.swapItems(InventoryIndex.Armor.Chest, elytraIdx)

                // 飛行開始パケット
                minecraft.connection?.send(
                    ServerboundPlayerCommandPacket(
                        player,
                        ServerboundPlayerCommandPacket.Action.START_FALL_FLYING,
                    ),
                )

                isElytraEquippedByFeature = true
                tickDelay = delay.value
                wasJumpKeyPressed = true
                return true
            }
        }

        wasJumpKeyPressed = isPressedJump
        return false
    }

    private fun handleAutoEquip(inv: InventorySystem): Boolean {
        val armorSlots = mapOf(
            EquipmentSlot.HEAD to InventoryIndex.Armor.Head,
            EquipmentSlot.CHEST to InventoryIndex.Armor.Chest,
            EquipmentSlot.LEGS to InventoryIndex.Armor.Legs,
            EquipmentSlot.FEET to InventoryIndex.Armor.Foots,
        )

        for ((slot, armorIdx) in armorSlots) {
            val currentArmor = inv.getItem(armorIdx)
            var bestScore = calculateArmorScore(currentArmor)
            var bestIndex: InventoryIndex? = null

            for (i in 0..35) {
                val invIdx = if (i < 9) InventoryIndex.Hotbar(i) else InventoryIndex.Backpack(i - 9)
                val stack = inv.getItem(invIdx)
                if (stack.isEmpty || getDesiredSlot(stack) != slot || isLowDurability(stack)) continue

                val score = calculateArmorScore(stack)
                if (score > bestScore) {
                    bestScore = score
                    bestIndex = invIdx
                }
            }

            if (bestIndex != null) {
                inv.swapItems(armorIdx, bestIndex)
                tickDelay = delay.value
                return true
            }
        }
        return false
    }

    private fun calculateArmorScore(stack: ItemStack): Int {
        if (stack.isEmpty || stack.item == Items.ELYTRA) return 0
        val item = stack.item

        val armorPoints = armorDefenceValues[item] ?: 0
        val toughness = armorToughnessValues[item] ?: 0

        val prot = enchantLevel(stack, Enchantments.PROTECTION)
        val fireProt = enchantLevel(stack, Enchantments.FIRE_PROTECTION)
        val blastProt = enchantLevel(stack, Enchantments.BLAST_PROTECTION)
        val projProt = enchantLevel(stack, Enchantments.PROJECTILE_PROTECTION)

        return (armorPoints * 5) + ((toughness + fireProt + blastProt + projProt) * 2) + (prot * 3)
    }

    private fun equipBackOriginalChest(inv: InventorySystem): Boolean {
        val chestIdx = InventoryIndex.Armor.Chest

        // 1. 元のアイテムを探す
        for (i in 0..35) {
            val invIdx = if (i < 9) InventoryIndex.Hotbar(i) else InventoryIndex.Backpack(i - 9)
            if (ItemStack.isSameItemSameComponents(inv.getItem(invIdx), previousChestplate)) {
                inv.swapItems(chestIdx, invIdx)
                return true
            }
        }

        // 2. なければ最強のチェストプレートを探して装備する（空きスロットへ脱ぐ代わり）
        var bestIdx: InventoryIndex? = null
        var bestScore = -1
        for (i in 0..35) {
            val invIdx = if (i < 9) InventoryIndex.Hotbar(i) else InventoryIndex.Backpack(i - 9)
            val stack = inv.getItem(invIdx)
            if (getDesiredSlot(stack) == EquipmentSlot.CHEST) {
                val score = calculateArmorScore(stack)
                if (score > bestScore) {
                    bestScore = score
                    bestIdx = invIdx
                }
            }
        }

        if (bestIdx != null) {
            inv.swapItems(chestIdx, bestIdx)
            return true
        }

        // 3. 最終手段：空きスロットへ脱ぐ
        for (i in 0..35) {
            val invIdx = if (i < 9) InventoryIndex.Hotbar(i) else InventoryIndex.Backpack(i - 9)
            if (inv.getItem(invIdx).isEmpty) {
                inv.swapItems(chestIdx, invIdx)
                return true
            }
        }
        return false
    }

    private fun findBestElytra(inv: InventorySystem): InventoryIndex? {
        var bestIdx: InventoryIndex? = null
        var maxDurability = -1
        for (i in 0..35) {
            val idx = if (i < 9) InventoryIndex.Hotbar(i) else InventoryIndex.Backpack(i - 9)
            val stack = inv.getItem(idx)
            if (stack.item == Items.ELYTRA && !isLowDurability(stack)) {
                val dur = stack.maxDamage - stack.damageValue
                if (dur > maxDurability) {
                    maxDurability = dur
                    bestIdx = idx
                }
            }
        }
        return bestIdx
    }

    private fun isLowDurability(stack: ItemStack): Boolean {
        if (!stack.isDamageableItem) return false
        val pct = (stack.maxDamage - stack.damageValue).toFloat() / stack.maxDamage * 100
        return pct < durabilityThreshold.value
    }

    private fun getDesiredSlot(stack: ItemStack): EquipmentSlot? {
        if (stack.isEmpty) return null
        val item = stack.item
        if (item == Items.ELYTRA) return EquipmentSlot.CHEST
        val equippable = stack.get(DataComponents.EQUIPPABLE)
        if (equippable != null) return equippable.slot
        return null
    }

    override fun onEnabled() {
        tickDelay = 0
        isElytraEquippedByFeature = false
        previousChestplate = ItemStack.EMPTY
    }
}
