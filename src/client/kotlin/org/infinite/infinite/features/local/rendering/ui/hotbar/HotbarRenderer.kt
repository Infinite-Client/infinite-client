package org.infinite.infinite.features.local.rendering.ui.hotbar

import net.minecraft.world.entity.HumanoidArm
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.rendering.ui.IUiRenderer
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.minecraft.multiplayer.inventory.InventorySystem
import org.infinite.libs.minecraft.multiplayer.inventory.structs.InventoryIndex
import org.infinite.utils.alpha

class HotbarRenderer :
    MinecraftInterface(),
    IUiRenderer {

    private val ultraUiFeature: UltraUiFeature
        get() = InfiniteClient.localFeatures.rendering.ultraUiFeature

    // アニメーション用：選択スロットの滑らかな移動
    private var animatedSelectedX = -1f

    override fun render(graphics2D: Graphics2D) {
        val player = player ?: return
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val alphaValue = ultraUiFeature.alpha.value
        val uiScale = ultraUiFeature.effectiveUiScale(graphics2D)

        val slotSize = 20f
        val totalWidth = 182f
        val totalHeight = 22f
        val offhandGap = 4f

        val isLeftHanded = player.mainArm == HumanoidArm.LEFT

        val (mainStartX, startY) = ultraUiFeature.hotbarOrigin(graphics2D)

        val targetSelectedX = 1f + (player.inventory.selectedSlot * slotSize)
        if (animatedSelectedX == -1f) animatedSelectedX = targetSelectedX
        animatedSelectedX += (targetSelectedX - animatedSelectedX) * 0.5f

        graphics2D.push()
        graphics2D.translate(mainStartX, startY)
        graphics2D.scale(uiScale, uiScale)

        theme.renderBackGround(0f, 0f, totalWidth, totalHeight, graphics2D, alphaValue)

        graphics2D.strokeStyle.width = 1f
        graphics2D.fillStyle = colorScheme.accentColor.alpha((255 * alphaValue).toInt())
        graphics2D.strokeRect(0f, 0f, totalWidth, totalHeight)
        graphics2D.fillStyle = colorScheme.accentColor.alpha((100 * alphaValue).toInt())
        graphics2D.fillRect(animatedSelectedX + 0.5f, 1.5f, 19f, 19f)

        for (i in 0 until 9) {
            val slotX = 1f + (i * slotSize)
            val slotY = 1f

            if (i < 8) {
                graphics2D.fillStyle = colorScheme.accentColor.alpha((60 * alphaValue).toInt())
                graphics2D.fillRect(slotX + slotSize - 1f, 4f, 1f, totalHeight - 8f)
            }

            val stack = InventorySystem.getItem(InventoryIndex.Hotbar(i))
            if (!stack.isEmpty) {
                graphics2D.itemCentered(stack, slotX + 10f, slotY + 10f, 16f)
            }
        }

        val offhandStack = player.offhandItem
        if (!offhandStack.isEmpty) {
            val offhandX = if (isLeftHanded) {
                totalWidth + offhandGap
            } else {
                -slotSize - 2f - offhandGap
            }

            theme.renderBackGround(offhandX, 0f, slotSize + 2f, totalHeight, graphics2D, alphaValue)
            graphics2D.strokeRect(offhandX, 0f, slotSize + 2f, totalHeight)

            graphics2D.itemCentered(offhandStack, offhandX + 11f, 11f, 16f)
        }

        graphics2D.pop()
    }
}
