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

/**
 * MDN CanvasRenderingContext2D API を Minecraft GuiGraphics 上に再現するクラス。
 * 座標指定を Float に統一し、Number 型を受け取ることで直感的なコーディングを可能にしています。
 */
@Suppress("Unused")
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
    private val fillOperations = Graphics2DPrimitivesFill(provider, { fillStyle }, { fillRule })
    private val strokeOperations = Graphics2DPrimitivesStroke(provider, { strokeStyle }, { enablePathGradient })
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
        strokeOperations.strokePath(path2D)
        path2D.clearSegments()
    }

    fun fillPath() {
        fillOperations.fillPath(path2D)
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

    fun fillRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        withTemporaryPath {
            val ss = strokeStyle.color
            strokeStyle.color = fillStyle
            roundedRectPath(x, y, width, height, radius)
            fillPath()
            strokeStyle.color = ss
        }
    }

    fun strokeRoundedRect(x: Number, y: Number, w: Number, h: Number, r: Number) = withTemporaryPath {
        roundedRectPath(
            x.toFloat(),
            y.toFloat(),
            w.toFloat(),
            h.toFloat(),
            r.toFloat(),
        )
        strokePath()
    }

    fun fillCircle(cx: Number, cy: Number, radius: Number) = withTemporaryPath {
        val ss = strokeStyle.color
        strokeStyle.color = fillStyle
        arc(cx, cy, radius, 0, PI * 2)
        fillPath()
        fillPath()
        strokeStyle.color = ss
    }

    fun strokeCircle(cx: Number, cy: Number, radius: Number) = withTemporaryPath {
        arc(cx, cy, radius, 0, PI * 2)
        strokePath()
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

    // --- Private Helpers ---
    private fun roundedRectPath(x: Float, y: Float, w: Float, h: Float, r: Float) {
        val radius = min(r, min(w / 2f, h / 2f))
        moveTo(x + radius, y)
        lineTo(x + w - radius, y)
        arcTo(x + w, y, x + w, y + radius, radius)
        lineTo(x + w, y + h - radius)
        arcTo(x + w, y + h, x + w - radius, y + h, radius)
        lineTo(x + radius, y + h)
        arcTo(x, y + h, x, y + h - radius, radius)
        lineTo(x, y + radius)
        arcTo(x, y, x + radius, y, radius)
        closePath()
    }

    private inline fun withTemporaryPath(block: () -> Unit) {
        val snapshot = path2D.snapshot()
        val last = path2D.lastPointData
        val first = path2D.firstPointData
        path2D.beginPath()
        block()
        path2D.restore(snapshot, last, first)
    }

    // --- System ---
    fun clear() = provider.clear()
    open fun commands(): List<RenderCommand2D> = provider.commands()
}
