package org.infinite.infinite.features.local.inventory.control

import net.minecraft.world.item.Items
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.minecraft.multiplayer.inventory.InventorySystem
import org.infinite.libs.minecraft.multiplayer.inventory.structs.InventoryIndex
import kotlin.math.pow

class ContainerUtilFeature : LocalFeature() {

    override val defaultToggleKey: Int = 0

    val autoRocket by property(BooleanProperty(true))
    val instantStop by property(BooleanProperty(true))
    val hotbarRotate by property(BooleanProperty(true))

    private var previousOffhandItem = Items.AIR
    private var isRocketEquipped = false
    private val hotbarKeysWasPressed = BooleanArray(9) { false }

    private class SlotAnimation(var progress: Float = 1.0f)
    private val animations = Array(9) { SlotAnimation() }

    override fun onStartTick() {
        val player = player ?: return
        val inv = InventorySystem
        if (minecraft.screen != null) return

        if (autoRocket.value) handleAutoRocket(inv)

        if (instantStop.value && player.isFallFlying) {
            if (options.keyShift.isDown && options.keySprint.isDown) {
                player.stopFallFlying()
            }
        }

        if (hotbarRotate.value) handleHotbarRotation(inv)
    }

    private fun handleAutoRocket(inv: InventorySystem) {
        val player = player ?: return
        val offhandIdx = InventoryIndex.OffHand
        val currentOffhand = inv.getItem(offhandIdx).item

        if (player.isFallFlying) {
            if (!isRocketEquipped && currentOffhand != Items.FIREWORK_ROCKET) {
                for (i in 0..35) {
                    val searchIdx = if (i < 9) InventoryIndex.Hotbar(i) else InventoryIndex.Backpack(i - 9)
                    if (inv.getItem(searchIdx).item == Items.FIREWORK_ROCKET) {
                        previousOffhandItem = currentOffhand
                        inv.swapItems(offhandIdx, searchIdx)
                        isRocketEquipped = true
                        return
                    }
                }
            }
        } else if (isRocketEquipped) {
            for (i in 0..35) {
                val searchIdx = if (i < 9) InventoryIndex.Hotbar(i) else InventoryIndex.Backpack(i - 9)
                val stack = inv.getItem(searchIdx)
                if (stack.item == previousOffhandItem || stack.isEmpty) {
                    inv.swapItems(offhandIdx, searchIdx)
                    isRocketEquipped = false
                    return
                }
            }
        }
    }

    private fun handleHotbarRotation(inv: InventorySystem) {
        val player = player ?: return
        for (i in 0..8) {
            val key = options.keyHotbarSlots[i]
            if (key.isDown && !hotbarKeysWasPressed[i]) {
                if (player.inventory.selectedSlot == i) {
                    rotateSlot(inv, i)
                    animations[i].progress = 0f
                }
            }
            hotbarKeysWasPressed[i] = key.isDown
        }
    }

    private fun rotateSlot(inv: InventorySystem, slotIdx: Int) {
        val hotbarIdx = InventoryIndex.Hotbar(slotIdx)
        // 循環ロジック: 上から下へ押し出す
        for (row in 2 downTo 0) {
            val bpIdx = InventoryIndex.Backpack(slotIdx + (row * 9))
            inv.swapItems(hotbarIdx, bpIdx)
        }
    }

    /**
     * ホットバー上のアイテム回転ビジュアルを描画する公開関数
     */
    fun renderRotationVisuals(graphics2D: Graphics2D, inv: InventorySystem) {
        val screenW = graphics2D.width
        val screenH = graphics2D.height
        val hotbarLeft = screenW / 2 - 91

        for (i in 0..8) {
            val anim = animations[i]
            if (anim.progress < 1.0f) {
                anim.progress += 0.15f * graphics2D.realDelta
                if (anim.progress > 1.0f) anim.progress = 1.0f
            }

            // 進行度をイージング処理
            val easedProgress = anim.progress.pow(0.5f)
            val x = (hotbarLeft + i * 20 + 10).toFloat()
            val baseY = (screenH - 11).toFloat()

            for (row in 0..2) {
                val stack = inv.getItem(InventoryIndex.Backpack(i + (row * 9)))
                if (stack.isEmpty) continue

                // アニメーションを考慮した垂直距離の計算
                val distance = (row + 1) - (1.0f - easedProgress)
                val offsetY = -20f * distance
                val scale = 1.0f / (1.0f + distance * 0.3f)

                graphics2D.push()
                graphics2D.translate(x, baseY + offsetY)
                graphics2D.scale(scale, scale)

                // 距離に応じてアルファ値を下げる (奥にある演出)
                val alpha = (255 * (1.0f - (distance * 0.25f))).toInt().coerceIn(0, 255)
                graphics2D.fillStyle = (alpha shl 24) or 0xFFFFFF

                graphics2D.itemCentered(stack, 0f, 0f, 16f)
                graphics2D.pop()
            }
        }
    }

    override fun onEndUiRendering(graphics2D: Graphics2D) {
        if (hotbarRotate.value) {
            renderRotationVisuals(graphics2D, InventorySystem)
        }
    }

    override fun onEnabled() {
        isRocketEquipped = false
        hotbarKeysWasPressed.fill(false)
        animations.forEach { it.progress = 1.0f }
    }
}
