package org.infinite.libs.graphics

import net.minecraft.client.DeltaTracker
import org.infinite.libs.graphics.graphics2d.Graphics2DPrimitivesFill
import org.infinite.libs.graphics.graphics2d.Graphics2DPrimitivesStroke
import org.infinite.libs.graphics.graphics2d.Graphics2DTransformations
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.graphics.graphics2d.structs.TextStyle
import org.infinite.libs.graphics.graphics2d.system.Path2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.joml.Matrix3x2f
import java.util.*

/**
 * MDN CanvasRenderingContext2D API を Minecraft GuiGraphics 上に再現するクラス。
 * zIndex を排除し、呼び出し順（画家のアルゴリズム）に従って描画コマンドを保持します。
 */
open class Graphics2D(
    deltaTracker: DeltaTracker,
) : MinecraftInterface() {
    val gameDelta: Float = deltaTracker.gameTimeDeltaTicks
    val realDelta: Float = deltaTracker.realtimeDeltaTicks // Corrected typo here
    val width: Int = client.window.guiScaledWidth
    val height: Int = client.window.guiScaledHeight
    var strokeStyle: StrokeStyle? = null
    var fillStyle: Int = 0xFFFFFFFF.toInt()
    var textStyle: TextStyle = TextStyle()
    var enablePathGradient: Boolean = false // New property for gradient control

    private val commandQueue = LinkedList<RenderCommand>()

    // Path2Dのインスタンスを追加
    private val path2D = Path2D(commandQueue)

    // 変換行列
    private val transformMatrix = Matrix3x2f()

    // 変換行列を保存するためのスタック
    private val transformStack = Stack<Matrix3x2f>()

    // 新しい描画および変換機能のインスタンス
    private val fillOperations: Graphics2DPrimitivesFill = Graphics2DPrimitivesFill(commandQueue) { fillStyle }
    private val strokeOperations: Graphics2DPrimitivesStroke =
        Graphics2DPrimitivesStroke(commandQueue, { strokeStyle }, { enablePathGradient })
    private val transformations: Graphics2DTransformations = Graphics2DTransformations(transformMatrix, transformStack)

    // --- fillRect ---

    fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        fillOperations.fillRect(x, y, width, height)
    }

    // --- fillQuad ---

    fun fillQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        fillOperations.fillQuad(x0, y0, x1, y1, x2, y2, x3, y3)
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
        fillOperations.fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, col0, col1, col2, col3)
    }

    // --- fillTriangle ---

    fun fillTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        fillOperations.fillTriangle(x0, y0, x1, y1, x2, y2)
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
        fillOperations.fillTriangle(x0, y0, x1, y1, x2, y2, col0, col1, col2)
    }

    // --- strokeRect ---
    fun strokeRect(x: Float, y: Float, width: Float, height: Float) {
        strokeOperations.strokeRect(x, y, width, height)
    }

    fun strokeRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        col0: Int, // 左上
        col1: Int, // 右上
        col2: Int, // 右下
        col3: Int, // 左下
    ) {
        strokeOperations.strokeRect(x, y, w, h, col0, col1, col2, col3)
    }

    // --- strokeQuad ---
    fun strokeQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        strokeOperations.strokeQuad(x0, y0, x1, y1, x2, y2, x3, y3)
    }

    fun strokeQuad(
        ix0: Float,
        iy0: Float,
        ix1: Float,
        iy1: Float,
        ix2: Float,
        iy2: Float,
        ix3: Float,
        iy3: Float,
        icol0: Int,
        icol1: Int,
        icol2: Int,
        icol3: Int,
    ) {
        strokeOperations.strokeQuad(ix0, iy0, ix1, iy1, ix2, iy2, ix3, iy3, icol0, icol1, icol2, icol3)
    }

    // --- strokeTriangle ---
    fun strokeTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        strokeOperations.strokeTriangle(x0, y0, x1, y1, x2, y2)
    }

    fun strokeTriangle(
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
        strokeOperations.strokeTriangle(x0, y0, x1, y1, x2, y2, col0, col1, col2)
    }

    // --- Path API ---

    fun beginPath() {
        path2D.beginPath()
    }

    fun moveTo(x: Float, y: Float) {
        path2D.moveTo(x, y)
    }

    fun lineTo(x: Float, y: Float) {
        val style = strokeStyle ?: return
        path2D.lineTo(x, y, style)
    }

    fun closePath() {
        val style = strokeStyle ?: return
        path2D.closePath(style)
    }

    fun strokePath() {
        strokeOperations.strokePolyline(path2D.getSegments())
        path2D.clearSegments()
    }

    fun arc(x: Float, y: Float, radius: Float, startAngle: Float, endAngle: Float, counterclockwise: Boolean = false) {
        val style = strokeStyle ?: return
        path2D.arc(x, y, radius, startAngle, endAngle, counterclockwise, style)
    }

    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float) {
        val style = strokeStyle ?: return
        path2D.arcTo(x1, y1, x2, y2, radius, style)
    }

    fun bezierCurveTo(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float, x: Float, y: Float) {
        val style = strokeStyle ?: return
        path2D.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y, style)
    }

    /**
     * 現在の変換状態をスタックに保存します。
     */
    fun save() {
        transformations.save()
    }

    fun text(text: String, x: Float, y: Float) {
        val shadow = textStyle.shadow
        val size = textStyle.size
        val font = textStyle.font
        commandQueue.add(RenderCommand.Text(font, text, x, y, fillStyle, shadow, size))
    }

    fun textCentered(text: String, x: Float, y: Float) {
        val shadow = textStyle.shadow
        val size = textStyle.size
        val font = textStyle.font
        commandQueue.add(RenderCommand.TextCentered(font, text, x, y, fillStyle, shadow, size))
    }
    private fun pushTransformCommand() {
        commandQueue.add(RenderCommand.SetTransform(Matrix3x2f(transformMatrix)))
    }

    // 変換メソッドをオーバーライド/修正
    fun translate(x: Float, y: Float) {
        transformations.translate(x, y)
        pushTransformCommand()
    }

    fun transform(m00: Float, m10: Float, m01: Float, m11: Float, m02: Float, m12: Float) {
        transformations.transform(m00, m10, m01, m11, m02, m12)
        pushTransformCommand()
    }

    fun restore() {
        transformations.restore()
        pushTransformCommand() // 復元後も行列状態を同期
    }

    // --- クリッピング (GuiGraphics.enableScissor 準拠) ---
    fun enableScissor(x: Int, y: Int, width: Int, height: Int) {
        commandQueue.add(RenderCommand.EnableScissor(x, y, width, height))
    }

    fun disableScissor() {
        commandQueue.add(RenderCommand.DisableScissor)
    }

    /**
     * 登録された順にコマンドを取り出します
     */
    fun commands(): List<RenderCommand> = commandQueue.toList()
}
