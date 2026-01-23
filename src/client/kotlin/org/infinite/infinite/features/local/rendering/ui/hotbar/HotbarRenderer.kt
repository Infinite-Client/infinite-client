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

        // 1. レイアウト定数
        val slotSize = 20f
        val totalWidth = 182f
        val totalHeight = 22f
        val offhandGap = 4f // オフハンドとメインバーの隙間

        // 2. 利き手（MainHand）の判定
        val isLeftHanded = player.mainArm == HumanoidArm.LEFT

        // メインホットバーのX基準位置 (中央)
        val mainStartX = (graphics2D.width - totalWidth) / 2f
        val startY = graphics2D.height - totalHeight

        // 3. アニメーション計算 (選択中のスロット座標)
        val targetSelectedX = mainStartX + 1f + (player.inventory.selectedSlot * slotSize)
        if (animatedSelectedX == -1f) animatedSelectedX = targetSelectedX
        animatedSelectedX += (targetSelectedX - animatedSelectedX) * 0.5f // 追従スピード

        // --- A. メインホットバー描画 ---
        theme.renderBackGround(mainStartX, startY, totalWidth, totalHeight, graphics2D, alphaValue)

        // 外枠
        graphics2D.strokeStyle.width = 1f
        graphics2D.fillStyle = colorScheme.accentColor.alpha((255 * alphaValue).toInt())
        graphics2D.strokeRect(mainStartX, startY, totalWidth, totalHeight)
        // selected Item
        graphics2D.fillStyle = colorScheme.accentColor.alpha((100 * alphaValue).toInt())
        graphics2D.fillRect(animatedSelectedX + 0.5f, startY + 1.5f, 19f, 19f)

        // スロットとアイテム
        for (i in 0 until 9) {
            val slotX = mainStartX + 1 + (i * slotSize)
            val slotY = startY + 1

            // 区切り線 (accentColorの薄い線)
            if (i < 8) {
                graphics2D.fillStyle = colorScheme.accentColor.alpha((60 * alphaValue).toInt())
                graphics2D.fillRect(slotX + slotSize - 1f, startY + 4f, 1f, totalHeight - 8f)
            }

            // アイテム描画 (Graphics2DPrimitivesTexture側で個数や耐久値が処理される)
            val stack = InventorySystem.getItem(InventoryIndex.Hotbar(i))
            if (!stack.isEmpty) {
                graphics2D.itemCentered(stack, slotX + 10f, slotY + 10f, 16f)
            }
        }

        // --- B. オフハンドスロット描画 ---
        val offhandStack = player.offhandItem
        if (!offhandStack.isEmpty) {
            // 利き手が右なら左側に、左なら右側にオフハンドを配置
            val offhandX = if (isLeftHanded) {
                mainStartX + totalWidth + offhandGap
            } else {
                mainStartX - slotSize - 2f - offhandGap
            }

            // オフハンドの背景と枠
            theme.renderBackGround(offhandX, startY, slotSize + 2f, totalHeight, graphics2D, alphaValue)
            graphics2D.strokeRect(offhandX, startY, slotSize + 2f, totalHeight)

            // オフハンドアイテム
            graphics2D.itemCentered(offhandStack, offhandX + 11f, startY + 11f, 16f)
        }
    }
}
