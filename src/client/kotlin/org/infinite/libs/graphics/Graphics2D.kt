package org.infinite.libs.graphics

import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import org.infinite.libs.core.tick.RenderTicks
import org.infinite.libs.graphics.graphics2d.Graphics2DPrimitivesFill
import org.infinite.libs.graphics.graphics2d.Graphics2DPrimitivesStroke
import org.infinite.libs.graphics.graphics2d.Graphics2DPrimitivesTexture
import org.infinite.libs.graphics.graphics2d.Graphics2DTransformations
import org.infinite.libs.graphics.graphics2d.structs.*
import org.infinite.libs.graphics.graphics2d.system.Path2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.joml.Matrix4f
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sqrt

/**
 * MDN CanvasRenderingContext2D API を Minecraft GuiGraphics 上に再現するクラス。
 * 座標指定を Float に統一し、Number 型を受け取ることで直感的なコーディングを可能にしています。
 */
open class Graphics2D : MinecraftInterface() {
    private val deltaTracker: DeltaTracker by lazy { Minecraft.getInstance().deltaTracker }
    val gameDelta: Float get() = deltaTracker.gameTimeDeltaTicks
    val realDelta: Float get() = deltaTracker.realtimeDeltaTicks

    open val width: Int get() = minecraft.window.guiScaledWidth
    open val height: Int get() = minecraft.window.guiScaledHeight

    // --- State ---
    var strokeStyle: StrokeStyle = StrokeStyle()
    var fillStyle: Int = 0xFFFFFFFF.toInt()
    var fillRule: FillRule = FillRule.EvenOdd
    var textStyle: TextStyle = TextStyle()
    var enablePathGradient: Boolean = false

    val fovFactor: Float
        get() {
            val gameRenderer = minecraft.gameRenderer
            val camera = gameRenderer.mainCamera
            val fov = gameRenderer.getFov(camera, realDelta, true)
            val base = options.fov().get().toFloat()
            return fov / base
        }

    // --- Provider & Operations ---
    private val provider = RenderCommand2DProvider()
    private val transformations = Graphics2DTransformations(provider)
    private val fillOperations = Graphics2DPrimitivesFill(provider) { fillStyle }
    private val strokeOperations = Graphics2DPrimitivesStroke(provider) { strokeStyle }
    private val textureOperations = Graphics2DPrimitivesTexture(provider) { textStyle }
    private val path2D = Path2D()

    // --- Transformations ---
    fun push() = transformations.push()
    fun pop() = transformations.pop()
    fun translate(x: Number, y: Number) = transformations.translate(x.toFloat(), y.toFloat())
    fun rotate(angle: Number) = transformations.rotate(angle.toFloat())
    fun rotateDegrees(degrees: Number) = rotate(Math.toRadians(degrees.toDouble()).toFloat())
    fun scale(x: Number, y: Number) = transformations.scale(x.toFloat(), y.toFloat())
    fun transform(x: Number, y: Number, z: Number) = transformations.transform(x.toFloat(), y.toFloat(), z.toFloat())
    fun setTransform(m00: Number, m10: Number, m01: Number, m11: Number, m02: Number, m12: Number) = transformations.setTransform(
        m00.toFloat(),
        m10.toFloat(),
        m01.toFloat(),
        m11.toFloat(),
        m02.toFloat(),
        m12.toFloat(),
    )

    fun resetTransform() = transformations.resetTransform()
    fun rotateAt(angle: Number, px: Number, py: Number) {
        translate(px, py)
        rotate(angle)
        translate(-px.toFloat(), -py.toFloat())
    }

    // --- Primitives (Fill) ---
    fun fillRect(x: Number, y: Number, w: Number, h: Number) = fillOperations.fillRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())

    fun fillRect(x: Number, y: Number, w: Number, h: Number, c0: Int, c1: Int, c2: Int, c3: Int) = fillOperations.fillRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), c0, c1, c2, c3)

    fun fillQuad(x0: Number, y0: Number, x1: Number, y1: Number, x2: Number, y2: Number, x3: Number, y3: Number) = fillOperations.fillQuad(
        x0.toFloat(),
        y0.toFloat(),
        x1.toFloat(),
        y1.toFloat(),
        x2.toFloat(),
        y2.toFloat(),
        x3.toFloat(),
        y3.toFloat(),
    )

    fun fillQuad(
        x0: Number,
        y0: Number,
        x1: Number,
        y1: Number,
        x2: Number,
        y2: Number,
        x3: Number,
        y3: Number,
        c0: Int,
        c1: Int,
        c2: Int,
        c3: Int,
    ) = fillOperations.fillQuad(
        x0.toFloat(),
        y0.toFloat(),
        x1.toFloat(),
        y1.toFloat(),
        x2.toFloat(),
        y2.toFloat(),
        x3.toFloat(),
        y3.toFloat(),
        c0,
        c1,
        c2,
        c3,
    )

    fun fillTriangle(x0: Number, y0: Number, x1: Number, y1: Number, x2: Number, y2: Number) = fillOperations.fillTriangle(
        x0.toFloat(),
        y0.toFloat(),
        x1.toFloat(),
        y1.toFloat(),
        x2.toFloat(),
        y2.toFloat(),
        fillStyle,
        fillStyle,
        fillStyle,
    )

    fun fillTriangle(
        x0: Number,
        y0: Number,
        x1: Number,
        y1: Number,
        x2: Number,
        y2: Number,
        c0: Int,
        c1: Int,
        c2: Int,
    ) = fillOperations.fillTriangle(
        x0.toFloat(),
        y0.toFloat(),
        x1.toFloat(),
        y1.toFloat(),
        x2.toFloat(),
        y2.toFloat(),
        c0,
        c1,
        c2,
    )

    // --- Primitives (Stroke) ---
    fun strokeRect(x: Number, y: Number, w: Number, h: Number) = strokeOperations.strokeRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())

    fun strokeRect(x: Number, y: Number, w: Number, h: Number, c0: Int, c1: Int, c2: Int, c3: Int) = strokeOperations.strokeRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), c0, c1, c2, c3)

    fun strokeQuad(x0: Number, y0: Number, x1: Number, y1: Number, x2: Number, y2: Number, x3: Number, y3: Number) = strokeOperations.strokeQuad(
        x0.toFloat(),
        y0.toFloat(),
        x1.toFloat(),
        y1.toFloat(),
        x2.toFloat(),
        y2.toFloat(),
        x3.toFloat(),
        y3.toFloat(),
    )

    fun strokeQuad(
        x0: Number,
        y0: Number,
        x1: Number,
        y1: Number,
        x2: Number,
        y2: Number,
        x3: Number,
        y3: Number,
        c0: Int,
        c1: Int,
        c2: Int,
        c3: Int,
    ) = strokeOperations.strokeQuad(
        x0.toFloat(),
        y0.toFloat(),
        x1.toFloat(),
        y1.toFloat(),
        x2.toFloat(),
        y2.toFloat(),
        x3.toFloat(),
        y3.toFloat(),
        c0,
        c1,
        c2,
        c3,
    )

    fun strokeTriangle(x0: Number, y0: Number, x1: Number, y1: Number, x2: Number, y2: Number) = strokeOperations.strokeTriangle(
        x0.toFloat(),
        y0.toFloat(),
        x1.toFloat(),
        y1.toFloat(),
        x2.toFloat(),
        y2.toFloat(),
    )

    fun strokeTriangle(
        x0: Number,
        y0: Number,
        x1: Number,
        y1: Number,
        x2: Number,
        y2: Number,
        c0: Int,
        c1: Int,
        c2: Int,
    ) = strokeOperations.strokeTriangle(
        x0.toFloat(),
        y0.toFloat(),
        x1.toFloat(),
        y1.toFloat(),
        x2.toFloat(),
        y2.toFloat(),
        c0,
        c1,
        c2,
    )

    // --- Path API ---
    fun beginPath() = path2D.beginPath()
    fun moveTo(x: Number, y: Number) = path2D.moveTo(x.toFloat(), y.toFloat())
    fun lineTo(x: Number, y: Number) = path2D.lineTo(x.toFloat(), y.toFloat(), strokeStyle)
    fun closePath() = path2D.closePath(strokeStyle)
    fun strokePath() {
        path2D.strokePath(enablePathGradient) { start, end, outS, outE, inS, inE ->
            strokeOperations.drawColoredEdge(start, end, outS, outE, inS, inE)
        }
        path2D.clearSegments()
    }

    fun fillPath() {
        // Path2D内部で計算されたポリゴンデータを、Graphics2Dの描画命令へ転送する
        path2D.fillPath(
            fillRule = this.fillRule,
            fillTriangle = { x0, y0, x1, y1, x2, y2, c0, c1, c2 ->
                // fillOperations のメソッドを直接呼ぶ、または自身の fillTriangle を呼ぶ
                this.fillTriangle(x0, y0, x1, y1, x2, y2, c0, c1, c2)
            },
            fillQuad = { x0, y0, x1, y1, x2, y2, x3, y3, c0, c1, c2, c3 ->
                this.fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, c0, c1, c2, c3)
            },
        )
        // 描画が終わったらパスをクリアして、次の beginPath に備える
        path2D.clearSegments()
    }

    fun arc(x: Number, y: Number, r: Number, s: Number, e: Number, ccw: Boolean = false) = path2D.arc(x.toFloat(), y.toFloat(), r.toFloat(), s.toFloat(), e.toFloat(), ccw, strokeStyle)

    fun arcTo(x1: Number, y1: Number, x2: Number, y2: Number, r: Number) = path2D.arcTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), r.toFloat(), strokeStyle)

    fun bezierCurveTo(cp1x: Number, cp1y: Number, cp2x: Number, cp2y: Number, x: Number, y: Number) = path2D.bezierCurveTo(
        cp1x.toFloat(),
        cp1y.toFloat(),
        cp2x.toFloat(),
        cp2y.toFloat(),
        x.toFloat(),
        y.toFloat(),
        strokeStyle,
    )

    fun fillRoundedRect(x: Number, y: Number, w: Number, h: Number, r: Number) {
        val xf = x.toFloat()
        val yf = y.toFloat()
        val wf = w.toFloat()
        val hf = h.toFloat()
        val rf = min(r.toFloat(), min(wf / 2f, hf / 2f))

        if (rf <= 1e-3f) {
            fillRect(xf, yf, wf, hf)
            return
        }

        // 1. 中央の大きな長方形（上下の角丸を除いた胴体部分）
        // 左右に突き出す部分は含めず、完全に中央のブロックを描画
        fillRect(xf, yf + rf, wf, hf - 2 * rf)

        // 2. 上部と下部の角丸を含む水平ストリップの描画
        // これにより、角の扇形と、その間の水平な長方形を「一続きの台形」として描画できる
        val segments = (rf / 1.5f * Path2D.getQualityScale()).toInt().coerceIn(4, 32)

        for (i in 0 until segments) {
            val yStartRel = i.toFloat() / segments * rf
            val yEndRel = (i + 1).toFloat() / segments * rf

            // 円の方程式 x^2 + y^2 = r^2 から、中心からの水平距離を導出
            // 上の段のy座標に対するxのオフセット
            val hUpper = rf - yStartRel
            val wUpper = sqrt((rf * rf - hUpper * hUpper).coerceAtLeast(0f))

            // 下の段のy座標に対するxのオフセット
            val hLower = rf - yEndRel
            val wLower = sqrt((rf * rf - hLower * hLower).coerceAtLeast(0f))

            // --- 上側の角丸ストリップ ---
            // 左上の角、中央の隙間、右上の角を一つの Quad Strip として描画
            fillQuad(
                xf + rf - wUpper,
                yf + yStartRel, // 上左
                xf + wf - rf + wUpper,
                yf + yStartRel, // 上右
                xf + wf - rf + wLower,
                yf + yEndRel, // 下右
                xf + rf - wLower,
                yf + yEndRel, // 下左
            )

            // --- 下側の角丸ストリップ ---
            // 同様に、左下、中央、右下を繋いで描画
            fillQuad(
                xf + rf - wLower,
                yf + hf - yEndRel, // 上左
                xf + wf - rf + wLower,
                yf + hf - yEndRel, // 上右
                xf + wf - rf + wUpper,
                yf + hf - yStartRel, // 下右
                xf + rf - wUpper,
                yf + hf - yStartRel, // 下左
            )
        }
    }

    fun fillCircle(cx: Number, cy: Number, radius: Number) {
        val x0 = cx.toFloat()
        val y0 = cy.toFloat()
        val r = radius.toFloat()
        if (r <= 0f) return

        // 品質に基づく分割数（垂直方向の分割数）
        // 円の高さ(2r)に対して適切な解像度を決定
        val segments = (r * 2f / 1.5f * Path2D.getQualityScale()).toInt().coerceIn(8, 64)

        var lastY = -r
        var lastWidth = 0f

        for (i in 1..segments) {
            // -r から r まで垂直に走査
            val currentY = -r + (i.toFloat() / segments) * 2 * r

            // 円の方程式 x^2 + y^2 = r^2 より、各高さでの水平方向の幅(x)を求める
            // x = sqrt(r^2 - y^2)
            val currentWidth = sqrt((r * r - currentY * currentY).coerceAtLeast(0f))

            // 前のステップの点と現在のステップの点で4点(Quad)を構成
            // 矩形ではなく、左右が対称に窄まった「台形」として描画される
            fillQuad(
                x0 - lastWidth,
                y0 + lastY, // 上左
                x0 + lastWidth,
                y0 + lastY, // 上右
                x0 + currentWidth,
                y0 + currentY, // 下右
                x0 - currentWidth,
                y0 + currentY, // 下左
            )

            lastY = currentY
            lastWidth = currentWidth
        }
    }

    private val internalPath2D = Path2D() // 内部計算用に追加

    // 共通ヘルパー: ユーザーのパスを汚さずにパスベースの描画を行う
    private inline fun withInternalPath(block: Path2D.() -> Unit) {
        internalPath2D.beginPath()
        internalPath2D.block()
        // strokePath(enablePathGradient) { ... } のロジックをここで実行
        internalPath2D.strokePath(enablePathGradient) { start, end, outS, outE, inS, inE ->
            strokeOperations.drawColoredEdge(start, end, outS, outE, inS, inE)
        }
        internalPath2D.clearSegments()
    }

    fun strokeCircle(cx: Number, cy: Number, radius: Number) = withInternalPath {
        arc(cx.toFloat(), cy.toFloat(), radius.toFloat(), 0f, (PI * 2).toFloat(), false, strokeStyle)
        closePath(strokeStyle)
    }

    fun strokeRoundedRect(x: Number, y: Number, w: Number, h: Number, r: Number) = withInternalPath {
        val xf = x.toFloat()
        val yf = y.toFloat()
        val wf = w.toFloat()
        val hf = h.toFloat()
        val rf = min(r.toFloat(), min(wf / 2f, hf / 2f))

        // パスを構築
        moveTo(xf + rf, yf)
        lineTo(xf + wf - rf, yf, strokeStyle)
        arcTo(xf + wf, yf, xf + wf, yf + rf, rf, strokeStyle)
        lineTo(xf + wf, yf + hf - rf, strokeStyle)
        arcTo(xf + wf, yf + hf, xf + wf - rf, yf + hf, rf, strokeStyle)
        lineTo(xf + rf, yf + hf, strokeStyle)
        arcTo(xf, yf + hf, xf, yf + hf - rf, rf, strokeStyle)
        lineTo(xf, yf + rf, strokeStyle)
        arcTo(xf, yf, xf + rf, yf, rf, strokeStyle)
        closePath(strokeStyle)
    }

    // --- Text ---
    fun text(text: String, x: Number, y: Number) = provider.getText()
        .set(textStyle.font, text, x.toFloat(), y.toFloat(), fillStyle, textStyle.shadow, textStyle.size)

    fun textCentered(text: String, x: Number, y: Number) = provider.getTextCentered()
        .set(textStyle.font, text, x.toFloat(), y.toFloat(), fillStyle, textStyle.shadow, textStyle.size)

    fun textRight(text: String, x: Number, y: Number) = provider.getTextRight()
        .set(textStyle.font, text, x.toFloat(), y.toFloat(), fillStyle, textStyle.shadow, textStyle.size)

    // --- Utilities & Scissor ---
    fun enableScissor(x: Number, y: Number, w: Number, h: Number) = provider.getEnableScissor().set(x.toInt(), y.toInt(), w.toInt(), h.toInt())

    fun disableScissor() = provider.addStatic(RenderCommand2D.DisableScissor)

    // --- Texture (Image, Item, Block) ---
    fun item(stack: ItemStack, x: Number, y: Number, size: Number = 16f) = textureOperations.drawItem(stack, x.toFloat(), y.toFloat(), size.toFloat())

    fun itemCentered(stack: ItemStack, cx: Number, cy: Number, size: Number = 16f) = item(stack, cx.toFloat() - size.toFloat() / 2f, cy.toFloat() - size.toFloat() / 2f, size)

    fun block(block: Block, x: Number, y: Number, size: Number = 16f) = provider.getRenderBlock().set(block, x.toFloat(), y.toFloat(), size.toFloat())

    fun blockCentered(block: Block, cx: Number, cy: Number, size: Number = 16f) = block(block, cx.toFloat() - size.toFloat() / 2f, cy.toFloat() - size.toFloat() / 2f, size)

    fun image(
        image: Image,
        x: Number,
        y: Number,
        w: Number = image.width,
        h: Number = image.height,
        u: Int = 0,
        v: Int = 0,
        uw: Int = image.width,
        vh: Int = image.height,
        color: Int = -1,
    ) = textureOperations.drawTexture(image, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), u, v, uw, vh, color)

    fun imageCentered(
        image: Image,
        cx: Number,
        cy: Number,
        w: Number = image.width,
        h: Number = image.height,
        color: Int = -1,
    ) = image(
        image,
        cx.toFloat() - w.toFloat() / 2f,
        cy.toFloat() - h.toFloat() / 2f,
        w,
        h,
        0,
        0,
        image.width,
        image.height,
        color,
    )

    // --- World Projection ---
    fun projectWorldToScreen(worldPos: Vec3): Pair<Float, Float>? {
        val data = RenderTicks.latestProjectionData ?: return null
        val relX = (worldPos.x - data.cameraPos.x).toFloat()
        val relY = (worldPos.y - data.cameraPos.y).toFloat()
        val relZ = (worldPos.z - data.cameraPos.z).toFloat()
        val vec = org.joml.Vector4f(relX, relY, relZ, 1f).mul(Matrix4f(data.projectionMatrix).mul(data.modelViewMatrix))
        if (vec.w <= 0.05f) return null
        val x = (vec.x / vec.w + 1f) * 0.5f * data.scaledWidth.toFloat()
        val y = (1f - vec.y / vec.w) * 0.5f * data.scaledHeight.toFloat()
        return x to y
    }

    // --- System ---
    fun clear() = provider.clear()
    open fun commands(): List<RenderCommand2D> = provider.commands()
}
