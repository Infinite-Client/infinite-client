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
    deltaTracker: DeltaTracker, // コンストラクタ引数としてのみ受け取る
    var zIndex: Int = 0,
) {
    // インスタンス化された瞬間の値を不変(val)として保持
    private val capturedGameDelta: Float = deltaTracker.gameTimeDeltaTicks
    private val capturedRealDelta: Float = deltaTracker.realtimeDeltaTicks

    var strokeStyle: StrokeStyle? = null // 必要に応じて可変に
    private val commandQueue = PriorityBlockingQueue<RenderCommand>(100, compareBy { it.zIndex })

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

    // 保存された値を返す（スレッド安全）
    fun gameDelta(): Float = capturedGameDelta

    fun realDelta(): Float = capturedRealDelta

    fun poll(): RenderCommand? = commandQueue.poll()
}
