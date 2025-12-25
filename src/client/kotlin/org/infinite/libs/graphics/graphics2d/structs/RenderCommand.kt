package org.infinite.libs.graphics.graphics2d.structs

sealed interface RenderCommand {

    // 矩形の枠線 (頂点ごとに色を指定: 左上, 右上, 右下, 左下)
    data class StrokeRect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val strokeWidth: Float,
        val col0: Int, // 左上
        val col1: Int, // 右上
        val col2: Int, // 右下
        val col3: Int, // 左下
    ) : RenderCommand

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

    // 四角形の枠線
    data class StrokeQuad(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val x3: Float,
        val y3: Float,
        val strokeWidth: Float,
        val col0: Int, // 内側頂点色
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

    // 三角形の枠線
    data class StrokeTriangle(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val strokeWidth: Float,
        val col0: Int, // 内側頂点色
        val col1: Int,
        val col2: Int,
    ) : RenderCommand
}
