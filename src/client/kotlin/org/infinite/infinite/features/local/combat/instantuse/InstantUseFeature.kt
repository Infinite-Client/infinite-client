package org.infinite.infinite.features.local.combat.instantuse

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.EndCrystalItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.item.ExperienceBottleItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.ThrowablePotionItem
import net.minecraft.world.item.TridentItem
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.combat.LocalCombatCategory
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.list.ItemListProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.mixin.graphics.MinecraftAccessor
import org.infinite.utils.alpha
import org.lwjgl.glfw.GLFW
import kotlin.math.PI
import kotlin.math.min

/**
 * InstantUse - Advanced instant item usage feature
 *
 * Provides multiple modes for instant/fast item usage:
 * - RightClick: Removes right-click cooldown entirely
 * - FastPlace: Rapid successive placements with configurable speed
 * - Timed: Configurable delay between uses
 */
class InstantUseFeature : LocalFeature() {
    override val featureType = FeatureLevel.Cheat
    override val categoryClass = LocalCombatCategory::class
    override val defaultToggleKey: Int = GLFW.GLFW_KEY_I

    // ==================== Enums ====================

    enum class InstantMode {
        RightClick, // Standard instant - removes delay entirely
        FastPlace, // Rapid placements with speed multiplier
        Timed, // Custom configurable delay
    }

    // ==================== Properties ====================

    // Mode selection
    val mode by property(EnumSelectionProperty(InstantMode.RightClick))

    // Timing configuration
    val customDelay by property(IntProperty(2, 0, 20, "ticks"))
    val fastPlaceSpeed by property(IntProperty(3, 1, 10))

    // Item category toggles
    private val allowCrystals by property(BooleanProperty(true))
    private val allowExpBottles by property(BooleanProperty(true))
    private val allowBlocks by property(BooleanProperty(true))
    private val allowThrowables by property(BooleanProperty(true))
    private val allowFood by property(BooleanProperty(false))
    private val allowAll by property(BooleanProperty(false))

    // Advanced options
    private val customItems by property(ItemListProperty(emptyList()))
    val onlyWhenLockOn by property(BooleanProperty(false))
    private val checkOffhand by property(BooleanProperty(true))

    // ==================== State ====================

    private var fastPlaceTicks = 0
    private var lastActiveHand: InteractionHand? = null
    private var isCurrentlyActive = false

    // ==================== Initialization ====================

    init {
        defineAction("cycle_mode", GLFW.GLFW_KEY_UNKNOWN) {
            cycleMode()
        }
    }

    // ==================== Core Logic ====================

    override fun onStartTick() {
        if (!isEnabled()) {
            isCurrentlyActive = false
            return
        }

        val player = player ?: return

        // Check lock-on requirement
        if (onlyWhenLockOn.value && !InfiniteClient.localFeatures.combat.lockOnFeature.isEnabled()) {
            isCurrentlyActive = false
            return
        }

        // Check main hand
        val mainHandStack = player.mainHandItem
        val mainHandActive = shouldInstantUse(mainHandStack)

        // Check offhand if enabled
        val offHandStack = player.offhandItem
        val offHandActive = checkOffhand.value && shouldInstantUse(offHandStack)

        isCurrentlyActive = mainHandActive || offHandActive

        if (!isCurrentlyActive) {
            fastPlaceTicks = 0
            return
        }

        // Determine which hand is active
        lastActiveHand =
            if (mainHandActive) {
                InteractionHand.MAIN_HAND
            } else {
                InteractionHand.OFF_HAND
            }

        // Apply the instant use based on mode
        applyInstantUse()
    }

    private fun applyInstantUse() {
        val accessor = minecraft as MinecraftAccessor

        when (mode.value) {
            InstantMode.RightClick -> {
                // Complete removal of right-click delay
                accessor.setRightClickDelay(0)
            }

            InstantMode.FastPlace -> {
                // Rapid placements - set delay based on speed multiplier
                // Speed 10 = delay 0, Speed 1 = delay 3
                val calculatedDelay = maxOf(0, 4 - fastPlaceSpeed.value)
                accessor.setRightClickDelay(calculatedDelay)

                // Track ticks for UI display
                fastPlaceTicks++
            }

            InstantMode.Timed -> {
                // Custom configurable delay
                accessor.setRightClickDelay(customDelay.value)
            }
        }
    }

    private fun shouldInstantUse(itemStack: ItemStack): Boolean {
        if (itemStack.isEmpty) return false

        val item = itemStack.item

        // Allow all overrides everything
        if (allowAll.value) return true

        // Check custom items list first
        val itemId = BuiltInRegistries.ITEM.getKey(item).toString()
        if (customItems.value.contains(itemId)) return true

        // Check each category
        return when {
            item is EndCrystalItem && allowCrystals.value -> true
            item is ExperienceBottleItem && allowExpBottles.value -> true
            item is BlockItem && allowBlocks.value -> true
            isThrowable(item) && allowThrowables.value -> true
            isFoodItem(itemStack) && allowFood.value -> true
            else -> false
        }
    }

    private fun isThrowable(item: Item): Boolean = item is SnowballItem ||
        item is EggItem ||
        item is EnderpearlItem ||
        item is ThrowablePotionItem ||
        item is TridentItem

    private fun isFoodItem(stack: ItemStack): Boolean = stack.has(DataComponents.FOOD)

    private fun cycleMode() {
        val modes = InstantMode.entries
        val currentIndex = modes.indexOf(mode.value)
        val nextIndex = (currentIndex + 1) % modes.size
        mode.value = modes[nextIndex]
        fastPlaceTicks = 0
    }

    // ==================== UI Rendering ====================

    override fun onEndUiRendering(graphics2D: Graphics2D) {
        if (!isEnabled()) return

        val colorScheme = InfiniteClient.theme.colorScheme

        graphics2D.push()

        val cx = graphics2D.width / 2f
        val cy = graphics2D.height / 2f
        val indicatorRadius = 20f
        val indicatorOffset = 32f // Offset from crosshair

        // Draw mode indicator below crosshair
        val indicatorY = cy + indicatorOffset

        // Background arc (subtle)
        graphics2D.strokeStyle.color = colorScheme.surfaceColor.alpha(80)
        graphics2D.strokeStyle.width = 2.0f
        graphics2D.beginPath()
        graphics2D.arc(cx, indicatorY, indicatorRadius, 0f, (PI * 2).toFloat(), false)
        graphics2D.strokePath()

        // Active indicator arc (fills based on activity)
        if (isCurrentlyActive) {
            val fillProgress = when (mode.value) {
                InstantMode.RightClick -> 1.0f
                InstantMode.FastPlace -> min(1.0f, fastPlaceTicks / 20f)
                InstantMode.Timed -> if (customDelay.value > 0) 0.5f else 1.0f
            }

            graphics2D.strokeStyle.color = colorScheme.accentColor
            graphics2D.strokeStyle.width = 3.0f
            graphics2D.beginPath()
            graphics2D.arc(
                cx,
                indicatorY,
                indicatorRadius,
                -PI.toFloat() / 2,
                (fillProgress * 2 * PI.toFloat()) - PI.toFloat() / 2,
                false,
            )
            graphics2D.strokePath()
        }

        // Mode text
        val modeText = when (mode.value) {
            InstantMode.RightClick -> "INSTANT"
            InstantMode.FastPlace -> "FAST×${fastPlaceSpeed.value}"
            InstantMode.Timed -> "T:${customDelay.value}"
        }

        graphics2D.textStyle.size = 8f
        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.fillStyle =
            if (isCurrentlyActive) colorScheme.accentColor else colorScheme.foregroundColor.alpha(150)
        graphics2D.textCentered(modeText, cx, indicatorY + indicatorRadius + 8f)

        // Show active hand indicator
        if (isCurrentlyActive && lastActiveHand != null) {
            val handText = if (lastActiveHand == InteractionHand.MAIN_HAND) "M" else "O"
            graphics2D.textStyle.size = 6f
            graphics2D.fillStyle = colorScheme.foregroundColor.alpha(100)
            graphics2D.textCentered(handText, cx, indicatorY - 4f)
        }

        graphics2D.pop()
    }
}
