package org.infinite.infinite.features.local.inventory.control

import net.minecraft.world.item.Items
import org.infinite.InfiniteClient
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

    private var lastSelectedSlot: Int = 0 // -1ではなく0で初期化（またはonEnabledで同期）

    private fun handleHotbarRotation(inv: InventorySystem) {
        val player = player ?: return
        val currentSlot = player.inventory.selectedSlot

        // 全スロットのキー状態を更新しつつ、現在のスロットのキーを判定
        for (i in 0..8) {
            val key = options.keyHotbarSlots[i]
            val isPressed = key.isDown

            // 1. 今押されたのが「現在選択中のスロット」のキーである
            // 2. かつ、前回のスロットが今回のスロットと同じ（移動してきた瞬間ではない）
            if (isPressed && !hotbarKeysWasPressed[i]) {
                if (i == currentSlot && i == lastSelectedSlot) {
                    rotateSlot(inv, i)
                    animations[i].progress = 0f
                }
            }

            hotbarKeysWasPressed[i] = isPressed
        }

        lastSelectedSlot = currentSlot
    }
    private fun rotateSlot(inv: InventorySystem, slotIdx: Int) {
        val hotbar = InventoryIndex.Hotbar(slotIdx)
        val bp0 = InventoryIndex.Backpack(slotIdx) // 上段
        val bp1 = InventoryIndex.Backpack(slotIdx + 9) // 中段
        val bp2 = InventoryIndex.Backpack(slotIdx + 18) // 下段

        // 空のスロットであっても強制的に入れ替える
        // 順序: Hotbar <-> BP0 <-> BP1 <-> BP2
        // これにより、Hotbarにあったものが上から順に下に落ちていきます
        inv.swapItems(hotbar, bp0)
        inv.swapItems(hotbar, bp1)
        inv.swapItems(hotbar, bp2)
        InfiniteClient.localFeatures.inventory.itemRelocateFeature.updateTargetSlots(inv)
    }

    /**
     * ホットバー上のアイテム回転ビジュアルを描画する公開関数
     */
    fun renderRotationVisuals(graphics2D: Graphics2D, inv: InventorySystem) {
        val screenW = graphics2D.width
        val screenH = graphics2D.height
        val hotbarLeft = screenW / 2 - 91

        // UltraUiの状態を取得
        val isUltraUiEnabled = InfiniteClient.localFeatures.rendering.ultraUiFeature.isEnabled()

        for (i in 0..8) {
            val anim = animations[i]
            if (anim.progress < 1.0f) {
                anim.progress += 0.15f * graphics2D.realDelta
                if (anim.progress > 1.0f) anim.progress = 1.0f
            }

            val easedProgress = anim.progress.pow(0.5f)
            val x = (hotbarLeft + i * 20 + 10).toFloat()
            val baseY = (screenH - 16).toFloat()

            for (row in 0..2) {
                val stack = inv.getItem(InventoryIndex.Backpack(i + (row * 9)))
                if (stack.isEmpty) continue

                val distance = (row + 1) - (1.0f - easedProgress)
                val offsetY = -20f * distance

                // UltraUiが有効ならスケールは常に1.0f、無効なら距離に応じて縮小
                val scale = if (isUltraUiEnabled) 1.0f else 1.0f / (1.0f + distance * 0.3f)

                graphics2D.push()
                graphics2D.translate(x, baseY + offsetY)
                graphics2D.scale(scale, scale)

                // UltraUiが有効な場合は透明度も少し控えめにする（または一定にする）とより馴染みます
                val alphaFactor = if (isUltraUiEnabled) 0.15f else 0.25f
                val alpha = (255 * (1.0f - (distance * alphaFactor))).toInt().coerceIn(0, 255)

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
