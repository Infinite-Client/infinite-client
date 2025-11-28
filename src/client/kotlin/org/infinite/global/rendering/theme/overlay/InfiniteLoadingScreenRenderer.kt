package org.infinite.global.rendering.theme.overlay

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Util
import net.minecraft.util.math.ColorHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Recreates the Plymouth "next-loading.svg" animation in code:
 * - Uses the exact path keyframes from the SVG animate values.
 * - Uses the same animated gradient stops/colors and timings.
 *
 * Minecraft cannot render SVG natively, so we procedurally draw the animated path.
 */
object InfiniteLoadingScreenRenderer {
    private var fallbackCompleteTime: Long = -1
    private val pathFrames: List<List<Pair<Float, Float>>> by lazy { parsePathFrames(PATH_VALUES) }
    private val keyTimes: List<Float> by lazy { parseKeyTimes(KEY_TIMES) }

    @JvmStatic
    fun render(
        context: DrawContext,
        progress: Float,
        reloadStartTime: Long,
        reloadCompleteTime: Long,
        reloading: Boolean,
    ) {
        val now = Util.getMeasuringTimeMs()
        val completeMark =
            if (reloadCompleteTime > -1) {
                fallbackCompleteTime = -1
                reloadCompleteTime
            } else {
                if (!reloading && progress >= 0.999f) {
                    if (fallbackCompleteTime == -1L) fallbackCompleteTime = now
                } else {
                    fallbackCompleteTime = -1
                }
                fallbackCompleteTime
            }

        val fadeIn = if (reloadStartTime > -1) ((now - reloadStartTime).toFloat() / 400f).coerceIn(0f, 1f) else 1f
        val fadeOut = if (completeMark > -1) (1f - ((now - completeMark).toFloat() / 800f).coerceIn(0f, 1f)) else 1f
        val alpha = (fadeIn * fadeOut).coerceIn(0f, 1f)
        if (alpha <= 0f) return

        val width = context.scaledWindowWidth
        val height = context.scaledWindowHeight

        val textRenderer = MinecraftClient.getInstance().textRenderer
        val topColor = ColorHelper.getArgb((255 * alpha).roundToInt(), 8, 10, 14)
        val bottomColor = ColorHelper.getArgb((255 * alpha).roundToInt(), 0, 0, 0)
        context.fillGradient(0, 0, width, height, topColor, bottomColor)

        // Animate the path (SVG viewBox 120x60)
        val spinnerAreaWidth = (width * 0.6f).roundToInt()
        val spinnerAreaHeight = (height * 0.2f).roundToInt()
        val originX = (width - spinnerAreaWidth) / 2
        val originY = (height * 0.24f).roundToInt()
        val frameCount = pathFrames.size
        if (frameCount > 1 && keyTimes.size == frameCount) {
            val tNorm = ((now % 2000L).toFloat() / 2000f).coerceIn(0f, 0.9999f)
            val idx = findKeyframeIndex(keyTimes, tNorm)
            val t0 = keyTimes[idx]
            val t1 = keyTimes[(idx + 1) % frameCount]
            val frame0 = idx
            val frame1 = (idx + 1) % frameCount
            val span = if (t1 >= t0) t1 - t0 else (1f - t0) + t1
            val localT = if (span <= 0f) 0f else ((tNorm - t0 + (if (tNorm < t0) 1f else 0f)) / span).coerceIn(0f, 1f)

            val points = interpolateFrame(pathFrames[frame0], pathFrames[frame1], localT)

            val scaleX = spinnerAreaWidth / 120f
            val scaleY = spinnerAreaHeight / 60f
            val stroke = (10f * min(scaleX, scaleY)).coerceIn(6f, 14f)

            val totalSegments = max(1, points.size - 1)
            for (i in 0 until totalSegments) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val posT = i.toFloat() / totalSegments.toFloat()
                drawThickSegment(
                    context,
                    originX,
                    originY,
                    p0.first * scaleX,
                    p0.second * scaleY,
                    p1.first * scaleX,
                    p1.second * scaleY,
                    stroke,
                    gradientColor(now, posT, alpha),
                )
            }
        }

        val titleAlpha = (255 * alpha).roundToInt()
        val title = Text.literal("Infinite Client")
        context.drawCenteredTextWithShadow(
            textRenderer,
            title,
            width / 2,
            originY + spinnerAreaHeight + 12,
            ColorHelper.getArgb(titleAlpha, 255, 255, 255),
        )

        val subtitleText =
            if (reloading) {
                Text.literal("Loading resources...")
            } else {
                Text.literal("Finishing setup...")
            }
        context.drawCenteredTextWithShadow(
            textRenderer,
            subtitleText,
            width / 2,
            originY + spinnerAreaHeight + 26,
            ColorHelper.getArgb((210 * alpha).roundToInt(), 180, 180, 180),
        )

        val progressBarWidth = (width * 0.55f).roundToInt()
        val progressBarHeight = 7
        val progressBarX = (width - progressBarWidth) / 2
        val progressBarY = (height * 0.68f).roundToInt()
        val baseBarColor = ColorHelper.getArgb((80 * alpha).roundToInt(), 255, 255, 255)
        context.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + progressBarHeight, baseBarColor)

        val clampedProgress = progress.coerceIn(0f, 1f)
        val filledWidth = (progressBarWidth * clampedProgress).roundToInt()
        val fillColor = ColorHelper.getArgb((230 * alpha).roundToInt(), 255, 255, 255)
        if (filledWidth > 0) {
            context.fill(progressBarX, progressBarY, progressBarX + filledWidth, progressBarY + progressBarHeight, fillColor)
        }

        val percentText = "${(clampedProgress * 100f).roundToInt()}%"
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("$percentText • Infinite"),
            width / 2,
            progressBarY + progressBarHeight + 8,
            ColorHelper.getArgb((200 * alpha).roundToInt(), 210, 210, 210),
        )
    }

    private fun findKeyframeIndex(
        times: List<Float>,
        t: Float,
    ): Int {
        for (i in 0 until times.size - 1) {
            val t0 = times[i]
            val t1 = times[i + 1]
            if (t in t0..t1) return i
        }
        return times.size - 1
    }

    private fun interpolateFrame(
        a: List<Pair<Float, Float>>,
        b: List<Pair<Float, Float>>,
        t: Float,
    ): List<Pair<Float, Float>> {
        val size = min(a.size, b.size)
        val result = ArrayList<Pair<Float, Float>>(size)
        for (i in 0 until size) {
            val x = a[i].first + (b[i].first - a[i].first) * t
            val y = a[i].second + (b[i].second - a[i].second) * t
            result.add(x to y)
        }
        return result
    }

    private fun drawThickSegment(
        context: DrawContext,
        originX: Int,
        originY: Int,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        stroke: Float,
        color: Int,
    ) {
        val dx = x1 - x0
        val dy = y1 - y0
        val length = max(abs(dx), abs(dy)).coerceAtLeast(1f)
        val steps = max(4, (length / (stroke / 2f)).roundToInt())
        val half = stroke / 2f
        for (s in 0..steps) {
            val t = s.toFloat() / steps.toFloat()
            val px = x0 + dx * t
            val py = y0 + dy * t
            context.fill(
                (originX + px - half).roundToInt(),
                (originY + py - half).roundToInt(),
                (originX + px + half).roundToInt(),
                (originY + py + half).roundToInt(),
                color,
            )
        }
    }

    private fun gradientColor(
        now: Long,
        posT: Float,
        alpha: Float,
    ): Int {
        val cycle = (now % 4000L).toFloat() / 4000f
        val stop0 = animatedStopColor(STOP0_COLORS, cycle)
        val stop1 = animatedStopColor(STOP1_COLORS, cycle)
        val stop2 = animatedStopColor(STOP2_COLORS, cycle)

        val color =
            when {
                posT <= 0.5f -> lerpColor(stop0, stop1, (posT / 0.5f).coerceIn(0f, 1f))
                else -> lerpColor(stop1, stop2, ((posT - 0.5f) / 0.5f).coerceIn(0f, 1f))
            }
        val outAlpha = (color.alpha * alpha).roundToInt()
        return ColorHelper.getArgb(outAlpha, color.r, color.g, color.b)
    }

    private fun animatedStopColor(
        sequence: List<IntArray>,
        t: Float,
    ): RGBA {
        val times = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        val tt = t.coerceIn(0f, 0.9999f)
        var idx = times.indexOfLast { it <= tt }
        if (idx == -1) idx = 0
        val next = (idx + 1) % sequence.size
        val span = times[next] - times[idx]
        val local = if (span <= 0f) 0f else ((tt - times[idx]) / span).coerceIn(0f, 1f)
        val c0 = sequence[idx]
        val c1 = sequence[next]
        return lerpColor(
            RGBA(255, c0[0], c0[1], c0[2]),
            RGBA(255, c1[0], c1[1], c1[2]),
            local,
        )
    }

    private fun lerpColor(
        c0: RGBA,
        c1: RGBA,
        t: Float,
    ): RGBA {
        val r = (c0.r + (c1.r - c0.r) * t).roundToInt()
        val g = (c0.g + (c1.g - c0.g) * t).roundToInt()
        val b = (c0.b + (c1.b - c0.b) * t).roundToInt()
        val a = (c0.alpha + (c1.alpha - c0.alpha) * t).roundToInt()
        return RGBA(a, r, g, b)
    }

    private fun parsePathFrames(values: String): List<List<Pair<Float, Float>>> =
        values
            .trim()
            .lines()
            .filter { it.isNotBlank() }
            .map { line ->
                val cleaned = line.replace("M", " ").replace("L", " ").replace(";", " ")
                val parts = cleaned.trim().split(Regex("\\s+"))
                parts.mapNotNull { token ->
                    val xy = token.split(",")
                    if (xy.size == 2) {
                        xy[0].toFloatOrNull()?.let { x ->
                            xy[1].toFloatOrNull()?.let { y -> x to y }
                        }
                    } else {
                        null
                    }
                }
            }.filter { it.isNotEmpty() }

    private fun parseKeyTimes(values: String): List<Float> = values.trim().split(";").mapNotNull { it.trim().toFloatOrNull() }

    private data class RGBA(
        val alpha: Int,
        val r: Int,
        val g: Int,
        val b: Int,
    )

    private val STOP0_COLORS =
        listOf(
            intArrayOf(255, 255, 255),
            intArrayOf(255, 0, 0),
            intArrayOf(255, 255, 255),
            intArrayOf(0, 255, 255),
            intArrayOf(255, 255, 255),
        )
    private val STOP1_COLORS =
        listOf(
            intArrayOf(255, 255, 255),
            intArrayOf(255, 255, 0),
            intArrayOf(255, 255, 255),
            intArrayOf(0, 0, 255),
            intArrayOf(255, 255, 255),
        )
    private val STOP2_COLORS =
        listOf(
            intArrayOf(255, 255, 255),
            intArrayOf(255, 255, 0),
            intArrayOf(255, 255, 255),
            intArrayOf(255, 0, 255),
            intArrayOf(255, 255, 255),
        )

    // Path keyframes extracted from next-loading.svg animate values
    private const val PATH_VALUES =
        """
M 10,30L 10,30L 20,50L 50,50L 70,10L 100,10L 110,30;
M 10,30L 20,50L 20,50L 50,50L 70,10L 100,10L 110,30;
M 10,30L 20,50L 50,50L 50,50L 70,10L 100,10L 110,30;
M 10,30L 20,50L 50,50L 70,10L 70,10L 100,10L 110,30;
M 10,30L 20,50L 50,50L 70,10L 100,10L 100,10L 110,30;
M 10,30L 20,50L 50,50L 70,10L 100,10L 110,30L 110,30;
M 20,50L 20,50L 50,50L 70,10L 100,10L 110,30L 100,50;
M 20,50L 50,50L 50,50L 70,10L 100,10L 110,30L 100,50;
M 20,50L 50,50L 70,10L 70,10L 100,10L 110,30L 100,50;
M 20,50L 50,50L 70,10L 100,10L 100,10L 110,30L 100,50;
M 20,50L 50,50L 70,10L 100,10L 110,30L 110,30L 100,50;
M 20,50L 50,50L 70,10L 100,10L 110,30L 100,50L 100,50;
M 50,50L 50,50L 70,10L 100,10L 110,30L 100,50L 70,50;
M 50,50L 70,10L 70,10L 100,10L 110,30L 100,50L 70,50;
M 50,50L 70,10L 100,10L 100,10L 110,30L 100,50L 70,50;
M 50,50L 70,10L 100,10L 110,30L 110,30L 100,50L 70,50;
M 50,50L 70,10L 100,10L 110,30L 100,50L 100,50L 70,50;
M 50,50L 70,10L 100,10L 110,30L 100,50L 70,50L 70,50;
M 70,10L 70,10L 100,10L 110,30L 100,50L 70,50L 50,10;
M 70,10L 100,10L 100,10L 110,30L 100,50L 70,50L 50,10;
M 70,10L 100,10L 110,30L 110,30L 100,50L 70,50L 50,10;
M 70,10L 100,10L 110,30L 100,50L 100,50L 70,50L 50,10;
M 70,10L 100,10L 110,30L 100,50L 70,50L 70,50L 50,10;
M 70,10L 100,10L 110,30L 100,50L 70,50L 50,10L 50,10;
M 100,10L 100,10L 110,30L 100,50L 70,50L 50,10L 20,10;
M 100,10L 110,30L 110,30L 100,50L 70,50L 50,10L 20,10;
M 100,10L 110,30L 100,50L 100,50L 70,50L 50,10L 20,10;
M 100,10L 110,30L 100,50L 70,50L 70,50L 50,10L 20,10;
M 100,10L 110,30L 100,50L 70,50L 50,10L 50,10L 20,10;
M 100,10L 110,30L 100,50L 70,50L 50,10L 20,10L 20,10;
M 110,30L 110,30L 100,50L 70,50L 50,10L 20,10L 10,30;
M 110,30L 100,50L 100,50L 70,50L 50,10L 20,10L 10,30;
M 110,30L 100,50L 70,50L 70,50L 50,10L 20,10L 10,30;
M 110,30L 100,50L 70,50L 50,10L 50,10L 20,10L 10,30;
M 110,30L 100,50L 70,50L 50,10L 20,10L 20,10L 10,30;
M 110,30L 100,50L 70,50L 50,10L 20,10L 10,30L 10,30;
M 100,50L 100,50L 70,50L 50,10L 20,10L 10,30L 20,50;
M 100,50L 70,50L 70,50L 50,10L 20,10L 10,30L 20,50;
M 100,50L 70,50L 50,10L 50,10L 20,10L 10,30L 20,50;
M 100,50L 70,50L 50,10L 20,10L 20,10L 10,30L 20,50;
M 100,50L 70,50L 50,10L 20,10L 10,30L 10,30L 20,50;
M 100,50L 70,50L 50,10L 20,10L 10,30L 20,50L 20,50;
M 70,50L 70,50L 50,10L 20,10L 10,30L 20,50L 50,50;
M 70,50L 50,10L 50,10L 20,10L 10,30L 20,50L 50,50;
M 70,50L 50,10L 20,10L 20,10L 10,30L 20,50L 50,50;
M 70,50L 50,10L 20,10L 10,30L 10,30L 20,50L 50,50;
M 70,50L 50,10L 20,10L 10,30L 20,50L 20,50L 50,50;
M 70,50L 50,10L 20,10L 10,30L 20,50L 50,50L 50,50;
M 50,10L 50,10L 20,10L 10,30L 20,50L 50,50L 70,10;
M 50,10L 20,10L 20,10L 10,30L 20,50L 50,50L 70,10;
M 50,10L 20,10L 10,30L 10,30L 20,50L 50,50L 70,10;
M 50,10L 20,10L 10,30L 20,50L 20,50L 50,50L 70,10;
M 50,10L 20,10L 10,30L 20,50L 50,50L 50,50L 70,10;
M 50,10L 20,10L 10,30L 20,50L 50,50L 70,10L 70,10;
M 20,10L 20,10L 10,30L 20,50L 50,50L 70,10L 100,10;
M 20,10L 10,30L 10,30L 20,50L 50,50L 70,10L 100,10;
M 20,10L 10,30L 20,50L 20,50L 50,50L 70,10L 100,10;
M 20,10L 10,30L 20,50L 50,50L 50,50L 70,10L 100,10;
M 20,10L 10,30L 20,50L 50,50L 70,10L 70,10L 100,10;
M 20,10L 10,30L 20,50L 50,50L 70,10L 100,10L 100,10;
M 10,30L 10,30L 20,50L 50,50L 70,10L 100,10L 110,30;
M 10,30L 20,50L 20,50L 50,50L 70,10L 100,10L 110,30;
M 10,30L 20,50L 50,50L 50,50L 70,10L 100,10L 110,30;
M 10,30L 20,50L 50,50L 70,10L 70,10L 100,10L 110,30;
M 10,30L 20,50L 50,50L 70,10L 100,10L 100,10L 110,30;
M 10,30L 20,50L 50,50L 70,10L 100,10L 110,30L 110,30;
M 20,10L 20,10L 10,30L 20,50L 50,50L 70,10L 100,10L 100,10
"""

    // keyTimes from the SVG animate element
    private const val KEY_TIMES =
        """
0.0000000000;
0.0000000000;
0.0000000000;
0.0000000000;
0.0000000000;
0.0000000000;
0.1000000000;
0.1000000000;
0.1000000000;
0.1000000000;
0.1000000000;
0.1000000000;
0.2000000000;
0.2000000000;
0.2000000000;
0.2000000000;
0.2000000000;
0.2000000000;
0.3000000000;
0.3000000000;
0.3000000000;
0.3000000000;
0.3000000000;
0.3000000000;
0.4000000000;
0.4000000000;
0.4000000000;
0.4000000000;
0.4000000000;
0.4000000000;
0.5000000000;
0.5000000000;
0.5000000000;
0.5000000000;
0.5000000000;
0.5000000000;
0.6000000000;
0.6000000000;
0.6000000000;
0.6000000000;
0.6000000000;
0.6000000000;
0.7000000000;
0.7000000000;
0.7000000000;
0.7000000000;
0.7000000000;
0.7000000000;
0.8000000000;
0.8000000000;
0.8000000000;
0.8000000000;
0.8000000000;
0.8000000000;
0.9000000000;
0.9000000000;
0.9000000000;
0.9000000000;
0.9000000000;
0.9000000000;
1.0000000000;
1.0000000000;
1.0000000000;
1.0000000000;
1.0000000000;
1.0000000000;
1
"""
}
