package org.infinite.libs.graphics.graphics2d

import net.minecraft.world.item.ItemStack
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.graphics2d.structs.Image
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.graphics.graphics2d.structs.TextStyle
import java.util.*

class Graphics2DPrimitivesTexture(
    private val commandQueue: LinkedList<RenderCommand2D>,
    private val textStyle: () -> TextStyle,
) {

    /**
     * アイテム描画をトランスフォームと同期してコマンド化します。
     * @param stack アイテム
     * @param x 基本X座標
     * @param y 基本Y座標
     * @param size 描画サイズ（デフォルト16f）。内部で scale に変換されます。
     */
    fun drawItem(stack: ItemStack, x: Float, y: Float, size: Float = 16f, alpha: Float = 1f) {
        if (stack.isEmpty) return
        val colorScheme = InfiniteClient.theme.colorScheme
        val scale = size / 16f

        // 1. アイテム本体の描画コマンドを追加
        commandQueue.add(RenderCommand2D.RenderItem(stack, x, y, scale, alpha))

        // 2. 個数の描画コマンドを計算して追加
        if (stack.count > 1) {
            val text = stack.count.toString()
            // TextStyleなどは必要に応じてGraphics2Dから取得するか、デフォルト値を設定
            val style = textStyle()
            commandQueue.add(
                RenderCommand2D.TextCentered(
                    style.font,
                    text,
                    x + size, // 右下寄りに配置する場合は座標を調整
                    y + size,
                    0xFFFFFFFF.toInt(), // 色（アルファ適用が必要なら計算）
                    style.shadow,
                    8f * scale,
                ),
            )
        }

        // 3. 耐久値バーの描画コマンドを追加
        if (stack.isDamageableItem && stack.damageValue > 0) {
            val progress = (stack.maxDamage - stack.damageValue).toFloat() / stack.maxDamage.toFloat()
            val barHeight = 2f * scale
            val barY = y + size - barHeight
            val bg = colorScheme.backgroundColor
            commandQueue.add(RenderCommand2D.FillRect(x, barY, size, barHeight, bg))

            // 進捗バー
            val fillWidth = size * progress
            if (fillWidth > 0) {
                // 色計算ロジックをここに移動
                val color = colorScheme.color(360 * progress * 0.3f, 1f, 0.5f, alpha)
                commandQueue.add(RenderCommand2D.FillRect(x, barY, fillWidth, barHeight, color))
            }
        }
    }

    // 汎用テクスチャ描画
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
        commandQueue.add(
            RenderCommand2D.DrawTexture(
                image, x, y, width, height, u, v, uWidth, vHeight, color,
            ),
        )
    }
}
