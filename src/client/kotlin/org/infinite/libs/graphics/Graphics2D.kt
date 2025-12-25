package org.infinite.libs.graphics

import net.minecraft.client.DeltaTracker
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import java.util.concurrent.LinkedBlockingQueue

/**
 * MDN CanvasRenderingContext2D API を Minecraft GuiGraphics 上に再現するクラス。
 * zIndex を排除し、呼び出し順（画家のアルゴリズム）に従って描画コマンドを保持します。
 */
class Graphics2D(
    deltaTracker: DeltaTracker,
) {
    private val capturedGameDelta: Float = deltaTracker.gameTimeDeltaTicks
    private val capturedRealDelta: Float = deltaTracker.realtimeDeltaTicks

    var strokeStyle: StrokeStyle? = null
    var fillStyle: Int = 0xFFFFFFFF.toInt()

    // zIndexによるソートが不要なため、単純なFIFOキューに変更
    // 100は初期容量ではなく、最大容量の指定になるため、必要に応じて調整してください
    private val commandQueue = LinkedBlockingQueue<RenderCommand>()

    // --- strokeRect ---

    fun strokeRect(x: Float, y: Float, width: Float, height: Float) {
        val style = strokeStyle ?: return
        val (strokeColor, strokeWidth) = style
        commandQueue.add(
            RenderCommand.StrokeRect(
                x,
                y,
                width,
                height,
                strokeWidth,
                strokeColor,
            ),
        )
    }

    // --- fillRect ---

    fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        commandQueue.add(RenderCommand.FillRect(x, y, width, height, fillStyle))
    }

    // --- fillQuad ---

    fun fillQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, fillStyle, fillStyle, fillStyle, fillStyle)
    }

    fun fillQuad(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        col0: Int,
        col1: Int,
        col2: Int,
        col3: Int,
    ) {
        commandQueue.add(
            RenderCommand.FillQuad(
                x0, y0, x1, y1, x2, y2, x3, y3,
                col0, col1, col2, col3,
            ),
        )
    }

    // --- fillTriangle ---

    fun fillTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        fillTriangle(x0, y0, x1, y1, x2, y2, fillStyle, fillStyle, fillStyle)
    }

    fun fillTriangle(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        col0: Int,
        col1: Int,
        col2: Int,
    ) {
        commandQueue.add(
            RenderCommand.FillTriangle(
                x0, y0, x1, y1, x2, y2,
                col0, col1, col2,
            ),
        )
    }

    // --- strokeQuad / strokeTriangle ---

    fun strokeQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val style = strokeStyle ?: return
        commandQueue.add(
            RenderCommand.StrokeQuad(
                x0, y0, x1, y1, x2, y2, x3, y3,
                style.width, style.color,
            ),
        )
    }

    fun strokeTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        val style = strokeStyle ?: return
        commandQueue.add(
            RenderCommand.StrokeTriangle(
                x0,
                y0,
                x1,
                y1,
                x2,
                y2,
                style.width,
                style.color,
            ),
        )
    }

    // --- Utilities ---

    fun gameDelta(): Float = capturedGameDelta
    fun realDelta(): Float = capturedRealDelta

    /**
     * 登録された順にコマンドを取り出します
     */
    fun commands(): List<RenderCommand> = commandQueue.toList()
}
