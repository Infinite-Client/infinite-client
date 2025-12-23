package org.infinite.libs.graphics

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.Identifier
import org.joml.Matrix4f

/**
 * MDN CanvasRenderingContext2D API を Minecraft GuiGraphics 上に再現するクラス
 */
class Graphics2D(
    private val gui: GuiGraphics,
) {
    // --- 型定義用の内部クラス・インターフェース ---

    interface CanvasStyle

    @JvmInline
    value class ColorStyle(
        val argb: Int,
    ) : CanvasStyle

    class LinearGradient(
        val x0: Double,
        val y0: Double,
        val x1: Double,
        val y1: Double,
    ) : CanvasStyle {
        private val colorStops = mutableListOf<Pair<Double, Int>>()

        fun addColorStop(
            offset: Double,
            color: Int,
        ) {
            colorStops.add(offset to color)
        }
    }

    class RadialGradient(
        val x0: Double,
        val y0: Double,
        val r0: Double,
        val x1: Double,
        val y1: Double,
        val r1: Double,
    ) : CanvasStyle {
        private val colorStops = mutableListOf<Pair<Double, Int>>()

        fun addColorStop(
            offset: Double,
            color: Int,
        ) {
            colorStops.add(offset to color)
        }
    }

    class CanvasPattern(
        val image: Identifier,
        val repetition: Repetition,
    ) : CanvasStyle

    data class TextMetrics(
        val width: Double,
        val actualBoundingBoxAscent: Double,
        val actualBoundingBoxDescent: Double,
    )

    // --- Enum定義 (キャメルケース) ---

    enum class Repetition { Repeat, RepeatX, RepeatY, NoRepeat }

    enum class LineCap { Butt, Round, Square }

    enum class LineJoin { Round, Bevel, Miter }

    enum class TextAlign { Start, End, Left, Right, Center }

    enum class TextBaseline { Top, Hanging, Middle, Alphabetic, Ideographic, Bottom }

    enum class Direction { Ltr, Rtl, Inherit }

    enum class ImageSmoothingQuality { Low, Medium, High }

    enum class FillRule { Nonzero, Evenodd }

    // --- インスタンスプロパティ ---

    var fillStyle: CanvasStyle = ColorStyle(0xFF000000.toInt())
    var strokeStyle: CanvasStyle = ColorStyle(0xFF000000.toInt())
    var lineWidth: Float = 1.0f
    var lineCap: LineCap = LineCap.Butt
    var lineJoin: LineJoin = LineJoin.Miter
    var miterLimit: Float = 10.0f
    var lineDashOffset: Float = 0.0f

    var font: String = "10px sans-serif"
    var textAlign: TextAlign = TextAlign.Start
    var textBaseline: TextBaseline = TextBaseline.Alphabetic
    var direction: Direction = Direction.Inherit

    var globalAlpha: Float = 1.0f
    var globalCompositeOperation: String = "source-over" // 複雑なためStringまたは専用Enum
    var imageSmoothingEnabled: Boolean = true
    var imageSmoothingQuality: ImageSmoothingQuality = ImageSmoothingQuality.Low

    var shadowBlur: Float = 0.0f
    var shadowColor: Int = 0x00000000
    var shadowOffsetX: Float = 0.0f
    var shadowOffsetY: Float = 0.0f

    var filter: String = "none"

    // --- メソッド ---

    // 矩形
    fun clearRect(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
    ) {}

    fun fillRect(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
    ) {}

    fun strokeRect(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
    ) {}

    // テキスト
    fun fillText(
        text: String,
        x: Double,
        y: Double,
        maxWidth: Double? = null,
    ) {}

    fun strokeText(
        text: String,
        x: Double,
        y: Double,
        maxWidth: Double? = null,
    ) {}

    fun measureText(text: String): TextMetrics = TextMetrics(0.0, 0.0, 0.0)

    // 線のスタイル
    fun getLineDash(): DoubleArray = doubleArrayOf()

    fun setLineDash(segments: DoubleArray) {}

    // グラデーションとパターン作成
    fun createLinearGradient(
        x0: Double,
        y0: Double,
        x1: Double,
        y1: Double,
    ): LinearGradient = LinearGradient(x0, y0, x1, y1)

    fun createRadialGradient(
        x0: Double,
        y0: Double,
        r0: Double,
        x1: Double,
        y1: Double,
        r1: Double,
    ): RadialGradient = RadialGradient(x0, y0, r0, x1, y1, r1)

    fun createPattern(
        image: Identifier,
        repetition: Repetition,
    ): CanvasPattern = CanvasPattern(image, repetition)

    // パス作成
    fun beginPath() {}

    fun closePath() {}

    fun moveTo(
        x: Double,
        y: Double,
    ) {}

    fun lineTo(
        x: Double,
        y: Double,
    ) {}

    fun bezierCurveTo(
        cp1x: Double,
        cp1y: Double,
        cp2x: Double,
        cp2y: Double,
        x: Double,
        y: Double,
    ) {}

    fun quadraticCurveTo(
        cpx: Double,
        cpy: Double,
        x: Double,
        y: Double,
    ) {}

    fun arc(
        x: Double,
        y: Double,
        radius: Double,
        startAngle: Double,
        endAngle: Double,
        counterclockwise: Boolean = false,
    ) {
    }

    fun arcTo(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        radius: Double,
    ) {}

    fun ellipse(
        x: Double,
        y: Double,
        radiusX: Double,
        radiusY: Double,
        rotation: Double,
        startAngle: Double,
        endAngle: Double,
        counterclockwise: Boolean = false,
    ) {
    }

    fun rect(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
    ) {}

    fun roundRect(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        radii: DoubleArray,
    ) {}

    // パス描画
    fun fill(fillRule: FillRule = FillRule.Nonzero) {}

    fun stroke() {}

    fun clip(fillRule: FillRule = FillRule.Nonzero) {}

    fun isPointInPath(
        x: Double,
        y: Double,
        fillRule: FillRule = FillRule.Nonzero,
    ): Boolean = false

    fun isPointInStroke(
        x: Double,
        y: Double,
    ): Boolean = false

    // 変形
    fun rotate(angle: Double) {}

    fun scale(
        x: Double,
        y: Double,
    ) {}

    fun translate(
        x: Double,
        y: Double,
    ) {}

    fun transform(
        a: Double,
        b: Double,
        c: Double,
        d: Double,
        e: Double,
        f: Double,
    ) {}

    fun getTransform(): Matrix4f = Matrix4f()

    fun setTransform(
        a: Double,
        b: Double,
        c: Double,
        d: Double,
        e: Double,
        f: Double,
    ) {}

    fun setTransform(matrix: Matrix4f) {}

    fun resetTransform() {}

    // 画像
    fun drawImage(
        image: Identifier,
        dx: Double,
        dy: Double,
    ) {}

    fun drawImage(
        image: Identifier,
        dx: Double,
        dy: Double,
        dw: Double,
        dh: Double,
    ) {}

    fun drawImage(
        image: Identifier,
        sx: Double,
        sy: Double,
        sw: Double,
        sh: Double,
        dx: Double,
        dy: Double,
        dw: Double,
        dh: Double,
    ) {
    }

    // 状態の保存
    fun save() {}

    fun restore() {}

    // その他
    fun reset() {}

    fun isContextLost(): Boolean = false
}
