package org.infinite.libs.graphics.graphics2d.structs

import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import org.joml.Matrix3x2f
import org.joml.Vector3f

sealed interface RenderCommand2D {
    // 矩形の塗りつぶし
    data class FillRect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val col0: Int, // 左上
        val col1: Int = col0, // 右上
        val col2: Int = col0, // 右下
        val col3: Int = col0, // 左下
    ) : RenderCommand2D

    // 四角形の塗りつぶし
    data class FillQuad(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val x3: Float,
        val y3: Float,
        val col0: Int,
        val col1: Int = col0,
        val col2: Int = col0,
        val col3: Int = col0,
    ) : RenderCommand2D

    // 三角形の塗りつぶし
    data class FillTriangle(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val col0: Int,
        val col1: Int = col0,
        val col2: Int = col0,
    ) : RenderCommand2D

    data class Text(
        val font: String,
        val text: String,
        val x: Float,
        val y: Float,
        val color: Int,
        val shadow: Boolean,
        val size: Float,
    ) : RenderCommand2D

    data class TextCentered(
        val font: String,
        val text: String,
        val x: Float,
        val y: Float,
        val color: Int,
        val shadow: Boolean,
        val size: Float,
    ) : RenderCommand2D

    data class TextRight(
        val font: String,
        val text: String,
        val x: Float,
        val y: Float,
        val color: Int,
        val shadow: Boolean,
        val size: Float,
    ) : RenderCommand2D

    object PushTransform : RenderCommand2D
    object PopTransform : RenderCommand2D

    // 行列を直接上書きするのではなく、現在の行列に対して操作を加えるコマンド群
    data class Translate(val x: Float, val y: Float) : RenderCommand2D
    data class Rotate(val angle: Float) : RenderCommand2D
    data class Scale(val x: Float, val y: Float) : RenderCommand2D
    data class Transform(
        val vec: Vector3f,
    ) : RenderCommand2D

    // 完全にリセット、または特定の行列に置き換える場合
    object ResetTransform : RenderCommand2D
    data class SetTransform(val matrix: Matrix3x2f) : RenderCommand2D

    data class EnableScissor(val x: Int, val y: Int, val width: Int, val height: Int) : RenderCommand2D
    object DisableScissor : RenderCommand2D
    data class DrawTexture(
        val image: Image,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val u: Int,
        val v: Int,
        val uWidth: Int,
        val vHeight: Int,
        val color: Int,
    ) : RenderCommand2D

    /**
     * アイテム描画コマンド
     * @param stack アイテム情報
     * @param x 描画座標
     * @param y 描画座標
     * @param scale 描画スケール（デフォルト1f）。
     */
    data class RenderItem(
        val stack: ItemStack,
        val x: Float,
        val y: Float,
        val scale: Float,
        val alpha: Float,
    ) : RenderCommand2D

    data class RenderBlock(
        val block: Block,
        val x: Float,
        val y: Float,
        val size: Float,
    ) : RenderCommand2D
    object BeginStencil : RenderCommand2D
    object StartMasking : RenderCommand2D
    object StopMasking : RenderCommand2D
    object DisableStencil : RenderCommand2D
}
