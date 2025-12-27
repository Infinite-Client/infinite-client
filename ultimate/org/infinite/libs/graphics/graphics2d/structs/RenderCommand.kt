package org.infinite.libs.graphics.graphics2d.structs

import org.joml.Matrix3x2f

sealed interface RenderCommand {
    // 矩形の塗りつぶし
    data class FillRect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val col0: Int, // 左上
        val col1: Int, // 右上
        val col2: Int, // 右下
        val col3: Int, // 左下
    ) : RenderCommand

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
        val col1: Int,
        val col2: Int,
        val col3: Int,
    ) : RenderCommand

    // 三角形の塗りつぶし
    data class FillTriangle(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val col0: Int,
        val col1: Int,
        val col2: Int,
    ) : RenderCommand

    data class Text(
        val font: String,
        val text: String,
        val x: Float,
        val y: Float,
        val color: Int,
        val shadow: Boolean,
        val size: Float,
    ) : RenderCommand
    data class TextCentered(
        val font: String,
        val text: String,
        val x: Float,
        val y: Float,
        val color: Int,
        val shadow: Boolean,
        val size: Float,
    ) : RenderCommand
    data class SetTransform(val matrix: Matrix3x2f) : RenderCommand
    data class EnableScissor(val x: Int, val y: Int, val width: Int, val height: Int) : RenderCommand
    object DisableScissor : RenderCommand
}
