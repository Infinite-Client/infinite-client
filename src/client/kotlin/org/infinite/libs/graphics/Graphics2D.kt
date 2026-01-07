package org.infinite.libs.graphics

import net.minecraft.client.DeltaTracker
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.infinite.libs.core.tick.RenderTicks
import org.infinite.libs.graphics.graphics2d.Graphics2DPrimitivesFill
import org.infinite.libs.graphics.graphics2d.Graphics2DPrimitivesStroke
import org.infinite.libs.graphics.graphics2d.Graphics2DPrimitivesTexture
import org.infinite.libs.graphics.graphics2d.Graphics2DTransformations
import org.infinite.libs.graphics.graphics2d.structs.Image
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.graphics.graphics2d.structs.TextStyle
import org.infinite.libs.graphics.graphics2d.system.Path2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.joml.Matrix4f
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
    open val width: Int = minecraft.window.guiScaledWidth
    open val height: Int = minecraft.window.guiScaledHeight
    var strokeStyle: StrokeStyle = StrokeStyle()
    var fillStyle: Int = 0xFFFFFFFF.toInt()
    var textStyle: TextStyle = TextStyle()
    var enablePathGradient: Boolean = false // New property for gradient control

    private val commandQueue = LinkedList<RenderCommand2D>()

    // Path2Dのインスタンスを追加
    private val path2D = Path2D()

    private val transformations: Graphics2DTransformations = Graphics2DTransformations(commandQueue)

    fun push() {
        transformations.push()
    }

    fun pop() {
        transformations.pop()
    }

    fun translate(x: Float, y: Float) {
        transformations.translate(x, y)
    }

    fun rotate(angle: Float) {
        transformations.rotate(angle)
    }

    fun rotateDegrees(degrees: Float) {
        rotate(Math.toRadians(degrees.toDouble()).toFloat())
    }

    fun scale(x: Float, y: Float) {
        transformations.scale(x, y)
    }

    fun transform(x: Float, y: Float, z: Float) {
        transformations.transform(x, y, z)
    }

    fun setTransform(m00: Float, m10: Float, m01: Float, m11: Float, m02: Float, m12: Float) {
        transformations.setTransform(m00, m10, m01, m11, m02, m12)
    }

    fun resetTransform() {
        transformations.resetTransform()
    }

    /**
     * 指定した座標を中心に回転させるユーティリティ
     */
    fun rotateAt(angle: Float, px: Float, py: Float) {
        translate(px, py)
        rotate(angle)
        translate(-px, -py)
    }

    // 新しい描画および変換機能のインスタンス
    private val fillOperations: Graphics2DPrimitivesFill = Graphics2DPrimitivesFill(commandQueue) { fillStyle }
    private val strokeOperations: Graphics2DPrimitivesStroke =
        Graphics2DPrimitivesStroke(commandQueue, { strokeStyle }, { enablePathGradient })
    private val textureOperations: Graphics2DPrimitivesTexture =
        Graphics2DPrimitivesTexture(commandQueue) { textStyle }

    // --- fillRect ---
    fun fillRect(x: Int, y: Int, width: Int, height: Int) =
        fillRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

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

    fun strokeRect(x: Int, y: Int, width: Int, height: Int) =
        strokeRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

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
        val style = strokeStyle
        path2D.lineTo(x, y, style)
    }

    fun closePath() {
        val style = strokeStyle
        path2D.closePath(style)
    }

    fun strokePath() {
        strokeOperations.strokePolyline(path2D.getSegments())
        path2D.clearSegments()
    }

    fun arc(x: Float, y: Float, radius: Float, startAngle: Float, endAngle: Float, counterclockwise: Boolean = false) {
        val style = strokeStyle
        path2D.arc(x, y, radius, startAngle, endAngle, counterclockwise, style)
    }

    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float) {
        val style = strokeStyle
        path2D.arcTo(x1, y1, x2, y2, radius, style)
    }

    fun bezierCurveTo(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float, x: Float, y: Float) {
        val style = strokeStyle
        path2D.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y, style)
    }

    fun text(text: String, x: Int, y: Int) = text(text, x.toFloat(), y.toFloat())
    fun text(text: String, x: Float, y: Float) {
        val shadow = textStyle.shadow
        val size = textStyle.size
        val font = textStyle.font
        commandQueue.add(RenderCommand2D.Text(font, text, x, y, fillStyle, shadow, size))
    }

    fun textCentered(text: String, x: Float, y: Float) {
        val shadow = textStyle.shadow
        val size = textStyle.size
        val font = textStyle.font
        commandQueue.add(RenderCommand2D.TextCentered(font, text, x, y, fillStyle, shadow, size))
    }

    // --- クリッピング (GuiGraphics.enableScissor 準拠) ---
    fun enableScissor(x: Int, y: Int, width: Int, height: Int) {
        commandQueue.add(RenderCommand2D.EnableScissor(x, y, width, height))
    }

    fun disableScissor() {
        commandQueue.add(RenderCommand2D.DisableScissor)
    }

    fun item(stack: ItemStack, x: Float, y: Float, size: Float = 16f) {
        textureOperations.drawItem(stack, x, y, size)
    }

    fun itemCentered(stack: ItemStack, x: Float, y: Float, size: Float) {
        textureOperations.drawItem(stack, x - size / 2, y - size / 2, size)
    }

    fun image(
        image: Image,
        x: Float,
        y: Float,
        width: Float = image.width.toFloat(),
        height: Float = image.height.toFloat(),
        u: Int = 0,
        v: Int = 0,
        uWidth: Int = image.width,
        vHeight: Int = image.height,
        color: Int = 0xFFFFFFFF.toInt(),
    ) {
        textureOperations.drawTexture(
            image, x, y, width, height, u, v, uWidth, vHeight, color,
        )
    }

    fun projectWorldToScreen(worldPos: Vec3): Pair<Double, Double>? {
        val data = RenderTicks.latestProjectionData ?: return null

        val relX = (worldPos.x - data.cameraPos.x).toFloat()
        val relY = (worldPos.y - data.cameraPos.y).toFloat()
        val relZ = (worldPos.z - data.cameraPos.z).toFloat()

        val targetVector = org.joml.Vector4f(relX, relY, relZ, 1.0f)

        // ViewProjection行列の合成
        val viewProjectionMatrix = Matrix4f(data.projectionMatrix).mul(data.modelViewMatrix)
        targetVector.mul(viewProjectionMatrix)

        val w = targetVector.w
        if (w <= 0.05f) return null

        val ndcX = targetVector.x / w
        val ndcY = targetVector.y / w

        if (ndcX < -1.0f || ndcX > 1.0f || ndcY < -1.0f || ndcY > 1.0f) return null

        val x = (ndcX + 1.0) * 0.5 * data.scaledWidth
        val y = (1.0 - ndcY) * 0.5 * data.scaledHeight

        return x to y
    }

    /**
     * 登録された順にコマンドを取り出します
     */
    open fun commands(): List<RenderCommand2D> = commandQueue.toList()
}
