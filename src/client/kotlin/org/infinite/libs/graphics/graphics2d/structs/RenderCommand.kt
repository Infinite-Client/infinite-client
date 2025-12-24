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
}
