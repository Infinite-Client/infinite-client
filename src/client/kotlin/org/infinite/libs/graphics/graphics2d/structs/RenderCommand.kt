package org.infinite.libs.graphics.graphics2d.structs

sealed interface RenderCommand {
    val zIndex: Int

    // --- DrawRect (枠線) ---

    data class DrawRectInt(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val strokeWidth: Int,
        val color: Int,
        override val zIndex: Int,
    ) : RenderCommand

    data class DrawRectFloat(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val strokeWidth: Float,
        val color: Int,
        override val zIndex: Int,
    ) : RenderCommand

    data class DrawRectDouble(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val strokeWidth: Double,
        val color: Int,
        override val zIndex: Int,
    ) : RenderCommand

    // --- FillRect (塗りつぶし) ---

    data class FillRectInt(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val color: Int,
        override val zIndex: Int,
    ) : RenderCommand

    data class FillRectFloat(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val color: Int,
        override val zIndex: Int,
    ) : RenderCommand

    data class FillRectDouble(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val color: Int,
        override val zIndex: Int,
    ) : RenderCommand

    data class FillQuadFloat(
        val x0: Float, val y0: Float,
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val x3: Float, val y3: Float,
        val col0: Int, val col1: Int, val col2: Int, val col3: Int,
        override val zIndex: Int,
    ) : RenderCommand

    data class FillQuadDouble(
        val x0: Double, val y0: Double,
        val x1: Double, val y1: Double,
        val x2: Double, val y2: Double,
        val x3: Double, val y3: Double,
        val col0: Int, val col1: Int, val col2: Int, val col3: Int,
        override val zIndex: Int,
    ) : RenderCommand

    // --- FillTriangle (三角形の塗りつぶし/グラデーション) ---
    data class FillTriangleFloat(
        val x0: Float, val y0: Float,
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val col0: Int, val col1: Int, val col2: Int,
        override val zIndex: Int,
    ) : RenderCommand

    data class FillTriangleDouble(
        val x0: Double, val y0: Double,
        val x1: Double, val y1: Double,
        val x2: Double, val y2: Double,
        val col0: Int, val col1: Int, val col2: Int,
        override val zIndex: Int,
    ) : RenderCommand
}
