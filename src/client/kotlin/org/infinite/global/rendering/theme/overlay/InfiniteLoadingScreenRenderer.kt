package org.infinite.global.rendering.theme.overlay

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.math.ColorHelper
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders an animated loading screen using a sequence of image textures.
 */
object InfiniteLoadingScreenRenderer {
    private var fallbackCompleteTime: Long = -1

    private const val ANIMATION_FRAMES_COUNT = 360
    private const val ANIMATION_FRAME_DURATION_MS = 30L
    private const val TEXTURE_SIZE = 512

    private val animationTextures: List<Identifier> by lazy {
        (0 until ANIMATION_FRAMES_COUNT).map { i ->
            Identifier.of("infinite", "textures/gui/loading_animations/animation-${String.format("%04d", i)}.png")
        }
    }

    @JvmStatic
    fun render(
        context: DrawContext,
        progress: Float,
        reloadStartTime: Long,
        reloadCompleteTime: Long,
        reloading: Boolean,
    ) {
        val g2d = Graphics2D(context) // wrapper for common drawing helpers
        val colors = InfiniteClient.currentColors()

        val now = Util.getMeasuringTimeMs()
        // Track completion moment even when vanilla reloadCompleteTime is missing
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
        val fadeOut = if (completeMark > -1) (1f - ((now - completeMark).toFloat() / 800f)).coerceIn(0f, 1f) else 1f
        val alpha = (fadeIn * fadeOut).coerceIn(0f, 1f)
        if (alpha <= 0f) return

        val width = g2d.width
        val height = g2d.height

        // Background gradient using themed colors
        val topColor = withAlpha(colors.backgroundColor, (220 * alpha).roundToInt())
        val bottomColor = withAlpha(colors.primaryColor, (140 * alpha).roundToInt())
        context.fillGradient(0, 0, width, height, topColor, bottomColor)

        // Animated spinner frames
        val currentFrameIndex = ((now / ANIMATION_FRAME_DURATION_MS) % ANIMATION_FRAMES_COUNT).toInt()
        val texture = animationTextures[currentFrameIndex]

        val displaySize = (min(width, height) * 0.5f).roundToInt()
        val textureX = (width - displaySize) / 2
        val textureY = (height - displaySize) / 2 - (height * 0.1f).roundToInt()

        g2d.drawRotatedTexture(
            identifier = texture,
            x = textureX.toFloat(),
            y = textureY.toFloat(),
            width = displaySize.toFloat(),
            height = displaySize.toFloat(),
            rotation = 0f,
            color = ColorHelper.getArgb((255 * alpha).roundToInt(), 255, 255, 255),
            u = 0f,
            v = 0f,
            uWidth = TEXTURE_SIZE.toFloat(),
            vHeight = TEXTURE_SIZE.toFloat(),
            textureWidth = TEXTURE_SIZE.toFloat(),
            textureHeight = TEXTURE_SIZE.toFloat(),
        )
        val fontHeight = g2d.fontHeight()
        val titleAlpha = withAlpha(colors.foregroundColor, (255 * alpha).roundToInt())
        val title = Text.literal("Infinite Client")
        g2d.centeredText(
            text = title,
            x = width / 2,
            y = textureY + displaySize + 3 * fontHeight,
            color = titleAlpha,
            shadow = true,
        )

        val subtitleText =
            if (reloading) {
                Text.literal("Loading resources...")
            } else {
                Text.literal("Finishing setup...")
            }
        val subtitleAlpha = withAlpha(colors.secondaryColor, (210 * alpha).roundToInt())
        g2d.centeredText(
            text = subtitleText,
            x = width / 2,
            y = textureY + displaySize + 4 * fontHeight,
            color = subtitleAlpha,
            shadow = true,
        )

        // Progress bar
        val progressBarWidth = (width * 0.55f).roundToInt()
        val progressBarX = (width - progressBarWidth) / 2
        val progressBarY = (height * 0.68f).roundToInt()

        val baseBarColor = withAlpha(colors.foregroundColor, (80 * alpha).roundToInt())
        g2d.rect(
            x1 = progressBarX.toFloat(),
            y1 = progressBarY.toFloat(),
            x2 = (progressBarX + progressBarWidth).toFloat(),
            y2 = (progressBarY + fontHeight).toFloat(),
            color = baseBarColor,
        )

        val clampedProgress = progress.coerceIn(0f, 1f)
        val filledWidth = (progressBarWidth * clampedProgress).roundToInt()
        val fillColor = withAlpha(colors.primaryColor, (230 * alpha).roundToInt())

        if (filledWidth > 0) {
            g2d.rect(
                x1 = progressBarX.toFloat(),
                y1 = progressBarY.toFloat(),
                x2 = (progressBarX + filledWidth).toFloat(),
                y2 = (progressBarY + fontHeight).toFloat(),
                color = fillColor,
            )
        }
        val percentText = "${(clampedProgress * 100f).roundToInt()}%"
        val percentTitle = Text.literal("${percentText} - Infinite")
        val percentAlpha = withAlpha(colors.foregroundColor, (200 * alpha).roundToInt())

        g2d.centeredText(
            text = percentTitle,
            x = width / 2,
            y = progressBarY + 4 * fontHeight,
            color = percentAlpha,
            shadow = true,
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        val clampedAlpha = alpha.coerceIn(0, 255)
        return (clampedAlpha shl 24) or (color and 0x00FFFFFF)
    }
}
