package org.infinite.infinite.features.local.combat.swapshot

import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.minecraft.multiplayer.inventory.InventorySystem
import org.infinite.libs.minecraft.multiplayer.inventory.structs.InventoryIndex
import org.infinite.utils.alpha
import org.lwjgl.glfw.GLFW
import kotlin.math.PI

class SwapShotFeature : LocalFeature() {
    enum class Mode { Shot, Reload }

    override val defaultToggleKey: Int = GLFW.GLFW_KEY_GRAVE_ACCENT
    private var mode = Mode.Reload
    val shotInterval by property(IntProperty(4, 1, 20, "ticks"))
    val offhandFireworks by property(BooleanProperty(true))
    val autoFire by property(BooleanProperty(true))

    private var tickDelay = 0
    private var actionPerformedThisTick = false

    init {
        defineAction("switch_mode", GLFW.GLFW_KEY_G) {
            mode = if (mode == Mode.Shot) Mode.Reload else Mode.Shot
            tickDelay = 0
        }
    }

    override fun onStartTick() {
        if (minecraft.screen != null || player == null) return
        val inv = InventorySystem
        actionPerformedThisTick = false

        // 1. インターバル管理
        if (tickDelay > 0) {
            tickDelay--
            return
        }

        if (offhandFireworks.value) handleOffhandFirework()

        val mainHandStack = inv.getItem(InventoryIndex.MainHand)

        // 2. メインハンドの処理
        processMainHand(inv, mainHandStack)

        // 3. 自動連射処理 (Shotモードで右クリック長押し中)
        if (mode == Mode.Shot && autoFire.value && minecraft.options.keyUse.isDown) {
            handleAutoFire()
        }

        // 4. ホットバーの整理（何もスワップしていない場合のみ）
        if (!actionPerformedThisTick) {
            cleanupHotbar(inv)
        }
    }

    private fun processMainHand(inv: InventorySystem, mainHandStack: ItemStack) {
        when (mode) {
            Mode.Shot -> {
                if (!isCharged(mainHandStack)) {
                    val nextLoaded = findCrossbowByState(loadRequired = true)
                    if (nextLoaded != null) {
                        // 次の弾があるなら即座にスワップ
                        performSwap(inv, InventoryIndex.MainHand, nextLoaded)
                        tickDelay = shotInterval.value
                    } else if (mainHandStack.item is CrossbowItem) {
                        // 在庫切れなら手を空にする
                        switchToEmptyHand(inv)
                        tickDelay = 2
                    }
                }
            }

            Mode.Reload -> {
                if (mainHandStack.item !is CrossbowItem || isCharged(mainHandStack)) {
                    val nextUnloaded = findCrossbowByState(loadRequired = false)
                    if (nextUnloaded != null) {
                        performSwap(inv, InventoryIndex.MainHand, nextUnloaded)
                        tickDelay = 1
                    } else if (mainHandStack.item is CrossbowItem) {
                        switchToEmptyHand(inv)
                        tickDelay = 2
                    }
                }
            }
        }
    }

    private fun handleAutoFire() {
        val player = player ?: return
        val stack = player.mainHandItem

        // 装填済みのクロスボウを持っているなら、右クリックを1回シミュレート
        if (isCharged(stack)) {
            minecraft.gameMode?.useItem(player, InteractionHand.MAIN_HAND)
            player.swing(InteractionHand.MAIN_HAND)
            // 発射直後にディレイを再設定（スワップが早すぎて不発するのを防ぐ）
            tickDelay = shotInterval.value
        }
    }

    private fun performSwap(inv: InventorySystem, from: InventoryIndex, to: InventoryIndex) {
        if (actionPerformedThisTick) return
        inv.swapItems(from, to)
        actionPerformedThisTick = true
    }

    private fun cleanupHotbar(inv: InventorySystem) {
        val mainSlot = InventoryIndex.MainHand.toContainerSlot()
        for (i in 0..8) {
            val hotbarIdx = InventoryIndex.Hotbar(i)
            if (hotbarIdx.toContainerSlot() == mainSlot) continue

            val stack = inv.getItem(hotbarIdx)
            if (stack.item is CrossbowItem) {
                val emptyBackpack = findRealEmptyBackpack(inv)
                if (emptyBackpack != null) {
                    performSwap(inv, hotbarIdx, emptyBackpack)
                    return
                }
            }
        }
    }

    private fun switchToEmptyHand(inv: InventorySystem) {
        if (actionPerformedThisTick) return
        val emptySlot = findRealEmptyBackpack(inv) ?: findEmptyHotbarSlot(inv)
        if (emptySlot != null && !inv.getItem(InventoryIndex.MainHand).isEmpty) {
            performSwap(inv, InventoryIndex.MainHand, emptySlot)
        }
    }

    private fun findRealEmptyBackpack(inv: InventorySystem): InventoryIndex? {
        for (i in 0..26) {
            val idx = InventoryIndex.Backpack(i)
            if (inv.getItem(idx).isEmpty) return idx
        }
        return null
    }

    private fun findEmptyHotbarSlot(inv: InventorySystem): InventoryIndex? {
        val mainSlot = InventoryIndex.MainHand.toContainerSlot()
        for (i in 0..8) {
            val idx = InventoryIndex.Hotbar(i)
            if (idx.toContainerSlot() == mainSlot) continue
            if (inv.getItem(idx).isEmpty) return idx
        }
        return null
    }

    private fun isCharged(stack: ItemStack): Boolean {
        if (stack.item !is CrossbowItem) return false
        return stack.get(DataComponents.CHARGED_PROJECTILES)?.isEmpty == false
    }

    private fun findCrossbowByState(loadRequired: Boolean): InventoryIndex? {
        val inv = InventorySystem
        val mainSlot = InventoryIndex.MainHand.toContainerSlot()
        // バックパックを優先して検索
        val targets = (0..26).map { InventoryIndex.Backpack(it) } + (0..8).map { InventoryIndex.Hotbar(it) }

        for (idx in targets) {
            if (idx.toContainerSlot() == mainSlot) continue
            val stack = inv.getItem(idx)
            if (stack.item is CrossbowItem && isCharged(stack) == loadRequired) return idx
        }
        return null
    }

    override fun onEndUiRendering(graphics2D: Graphics2D) {
        if (!isEnabled()) return
        val loaded = countCrossbows(onlyLoaded = true)
        val totalCrossbows = countCrossbows()
        if (totalCrossbows == 0) return

        val loadedPercentage = loaded / totalCrossbows.toFloat()
        val colorScheme = InfiniteClient.theme.colorScheme

        graphics2D.push()
        val cx = graphics2D.width / 2f
        val cy = graphics2D.height / 2f
        val crosshairRadius = 16f

        graphics2D.strokeStyle.color = (if (mode == Mode.Shot) colorScheme.foregroundColor else colorScheme.accentColor).alpha(100)
        graphics2D.strokeStyle.width = 1.0f
        graphics2D.beginPath()
        graphics2D.arc(cx, cy, crosshairRadius, 0f, (PI * 2).toFloat(), false)
        graphics2D.strokePath()
        if (loaded > 0) {
            graphics2D.strokeStyle.color = colorScheme.accentColor
            graphics2D.strokeStyle.width = 2.5f
            graphics2D.beginPath()
            graphics2D.arc(
                cx,
                cy,
                crosshairRadius,
                -PI.toFloat() / 2,
                (loadedPercentage * 2 * PI.toFloat()) - PI.toFloat() / 2,
                false,
            )
            graphics2D.strokePath()
        }
        graphics2D.textStyle.size = 10f
        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.fillStyle = if (mode == Mode.Shot) colorScheme.accentColor else colorScheme.foregroundColor
        graphics2D.textCentered(mode.name.uppercase(), cx, cy + crosshairRadius + 10f)
        graphics2D.pop()
    }

    private fun countCrossbows(onlyLoaded: Boolean? = null): Int {
        val inv = InventorySystem
        val targets = (0..8).map { InventoryIndex.Hotbar(it) } + (0..26).map { InventoryIndex.Backpack(it) }
        return targets.count { stackIdx ->
            val stack = inv.getItem(stackIdx)
            stack.item is CrossbowItem && (onlyLoaded == null || isCharged(stack) == onlyLoaded)
        }
    }

    private fun handleOffhandFirework() {
        val inv = InventorySystem
        if (inv.getItem(InventoryIndex.OffHand).item != Items.FIREWORK_ROCKET) {
            val fireworkIdx = inv.findFirst(Items.FIREWORK_ROCKET)
            if (fireworkIdx != null) performSwap(inv, InventoryIndex.OffHand, fireworkIdx)
        }
    }
}
