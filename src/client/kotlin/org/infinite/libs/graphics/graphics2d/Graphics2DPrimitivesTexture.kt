package org.infinite.libs.graphics.graphics2d

import net.minecraft.world.item.ItemStack
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.graphics2d.structs.*
import kotlin.math.roundToInt

class Graphics2DPrimitivesTexture(
    private val provider: RenderCommand2DProvider,
    private val getTextStyle: () -> TextStyle,
) {

    fun drawItem(stack: ItemStack, x: Float, y: Float, size: Float = 16f, alpha: Float = 1f) {
        if (stack.isEmpty) return
        val colorScheme = InfiniteClient.theme.colorScheme
        val scale = size / 16f

        // 1. アイテム本体 (RenderItem の set を使用)
        provider.getRenderItem().set(stack, x, y, scale, alpha)

        // 2. 個数の描画 (TextRight の set を使用)
        if (stack.count > 1) {
            val text = stack.count.toString()
            val fontSize = 9f * scale
            val style = getTextStyle()
            val pad = (fontSize * 0.75f).roundToInt()

            provider.getTextRight().set(
                style.font,
                text,
                x + size + pad / 3f,
                y + size - pad,
                colorScheme.foregroundColor,
                true,
                fontSize,
            )
        }

        // 3. 耐久値バー (FillRect の set を使用)
        if (stack.isDamageableItem && stack.damageValue > 0) {
            val progress = (stack.maxDamage - stack.damageValue).toFloat() / stack.maxDamage.toFloat()
            val barHeight = 2f * scale
            val barY = y + size - barHeight
            val bg = colorScheme.backgroundColor

            // 背景バー
            provider.getFillRect().set(x, barY, size, barHeight, bg, bg, bg, bg)

            // 進捗バー
            val fillWidth = size * progress
            if (fillWidth > 0) {
                val color = colorScheme.color(360 * progress * 0.3f, 1f, 0.5f, alpha)
                provider.getFillRect().set(x, barY, fillWidth, barHeight, color, color, color, color)
            }
        }
    }

    // 汎用テクスチャ描画 (DrawTexture の set を使用)
    fun drawTexture(
        image: Image,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        u: Int,
        v: Int,
        uWidth: Int,
        vHeight: Int,
        color: Int,
    ) {
        provider.getDrawTexture().set(image, x, y, width, height, u, v, uWidth, vHeight, color)
    }
}
