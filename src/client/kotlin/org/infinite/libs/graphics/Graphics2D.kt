package org.infinite.libs.graphics

import net.minecraft.client.DeltaTracker
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import java.util.concurrent.PriorityBlockingQueue
import kotlin.math.roundToInt

/**
 * MDN CanvasRenderingContext2D API を Minecraft GuiGraphics 上に再現するクラス
 */
class Graphics2D(
    deltaTracker: DeltaTracker,
    var zIndex: Int = 0,
) {
    private val capturedGameDelta: Float = deltaTracker.gameTimeDeltaTicks
    private val capturedRealDelta: Float = deltaTracker.realtimeDeltaTicks

    var strokeStyle: StrokeStyle? = null

    // 塗りつぶしの色（Canvas API風に Int で管理）
    var fillStyle: Int = 0xFFFFFFFF.toInt()

    private val commandQueue = PriorityBlockingQueue<RenderCommand>(100, compareBy { it.zIndex })

    // --- strokeRect ---

    fun strokeRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val style = strokeStyle ?: return
        val (strokeColor, strokeWidthDouble) = style
        val strokeWidth = strokeWidthDouble.roundToInt()
        commandQueue.add(RenderCommand.DrawRectInt(x, y, width, height, strokeWidth, strokeColor, zIndex))
    }

    fun strokeRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ) {
        val style = strokeStyle ?: return
        val (strokeColor, strokeWidthDouble) = style
        val strokeWidth = strokeWidthDouble.toFloat()
        commandQueue.add(RenderCommand.DrawRectFloat(x, y, width, height, strokeWidth, strokeColor, zIndex))
    }

    fun strokeRect(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ) {
        val style = strokeStyle ?: return
        val (strokeColor, strokeWidth) = style
        commandQueue.add(RenderCommand.DrawRectDouble(x, y, width, height, strokeWidth, strokeColor, zIndex))
    }

    // --- fillRect ---

    fun fillRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        commandQueue.add(RenderCommand.FillRectInt(x, y, width, height, fillStyle, zIndex))
    }

    fun fillRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ) {
        commandQueue.add(RenderCommand.FillRectFloat(x, y, width, height, fillStyle, zIndex))
    }

    fun fillRect(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ) {
        commandQueue.add(RenderCommand.FillRectDouble(x, y, width, height, fillStyle, zIndex))
    }

    fun fillQuad(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float
    ) {
        fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, fillStyle, fillStyle, fillStyle, fillStyle)
    }

    /**
     * 各頂点に個別の色を指定して四角形を塗りつぶします（グラデーション）。
     */
    fun fillQuad(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        col0: Int, col1: Int, col2: Int, col3: Int
    ) {
        commandQueue.add(
            RenderCommand.FillQuadFloat(
                x0, y0, x1, y1, x2, y2, x3, y3,
                col0, col1, col2, col3, zIndex
            )
        )
    }

    fun fillQuad(
        x0: Double, y0: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double,
        x3: Double, y3: Double
    ) {
        fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, fillStyle, fillStyle, fillStyle, fillStyle)
    }

    fun fillQuad(
        x0: Double, y0: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double,
        x3: Double, y3: Double,
        col0: Int, col1: Int, col2: Int, col3: Int
    ) {
        commandQueue.add(
            RenderCommand.FillQuadDouble(
                x0, y0, x1, y1, x2, y2, x3, y3,
                col0, col1, col2, col3, zIndex
            )
        )
    }

    // --- fillTriangle (三角形指定) ---

    /**
     * 現在の fillStyle を使用して三角形を塗りつぶします。
     */
    fun fillTriangle(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ) {
        fillTriangle(x0, y0, x1, y1, x2, y2, fillStyle, fillStyle, fillStyle)
    }

    /**
     * 各頂点に個別の色を指定して三角形を塗りつぶします（グラデーション）。
     */
    fun fillTriangle(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        col0: Int, col1: Int, col2: Int
    ) {
        commandQueue.add(
            RenderCommand.FillTriangleFloat(
                x0, y0, x1, y1, x2, y2,
                col0, col1, col2, zIndex
            )
        )
    }

    fun fillTriangle(
        x0: Double, y0: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double
    ) {
        fillTriangle(x0, y0, x1, y1, x2, y2, fillStyle, fillStyle, fillStyle)
    }

    fun fillTriangle(
        x0: Double, y0: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double,
        col0: Int, col1: Int, col2: Int
    ) {
        commandQueue.add(
            RenderCommand.FillTriangleDouble(
                x0, y0, x1, y1, x2, y2,
                col0, col1, col2, zIndex
            )
        )
    }
    // --- Utilities ---

    fun gameDelta(): Float = capturedGameDelta

    fun realDelta(): Float = capturedRealDelta

    fun poll(): RenderCommand? = commandQueue.poll()
}
