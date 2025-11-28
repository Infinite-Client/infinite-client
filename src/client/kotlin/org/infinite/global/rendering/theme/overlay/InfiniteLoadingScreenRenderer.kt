package org.infinite.global.rendering.theme.overlay

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.math.ColorHelper
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders an animated loading screen using a sequence of image textures.
 */
object InfiniteLoadingScreenRenderer {
    private var fallbackCompleteTime: Long = -1

    private const val ANIMATION_FRAMES_COUNT = 360 // animation-0000.png から animation-0359.png まで
    private const val ANIMATION_FRAME_DURATION_MS = 30L // フレーム表示時間 (ms)
    private const val TEXTURE_SIZE = 512 // テクスチャのサイズ (512x512)

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
        val fadeOut = if (completeMark > -1) (1f - ((now - completeMark).toFloat() / 800f)).coerceIn(0f, 1f) else 1f
        val alpha = (fadeIn * fadeOut).coerceIn(0f, 1f)
        if (alpha <= 0f) return

        val width = context.scaledWindowWidth
        val height = context.scaledWindowHeight

        val textRenderer = MinecraftClient.getInstance().textRenderer
        val topColor = ColorHelper.getArgb((255 * alpha).roundToInt(), 8, 10, 14)
        val bottomColor = ColorHelper.getArgb((255 * alpha).roundToInt(), 0, 0, 0)
        context.fillGradient(0, 0, width, height, topColor, bottomColor)

        // アニメーション画像の描画
        val currentFrameIndex = ((now / ANIMATION_FRAME_DURATION_MS) % ANIMATION_FRAMES_COUNT).toInt()
        val texture = animationTextures[currentFrameIndex]

        val displaySize = (min(width, height) * 0.5f).roundToInt() // 画面の小さい方に合わせて表示サイズを調整
        val textureX = (width - displaySize) / 2
        val textureY = (height - displaySize) / 2 - (height * 0.1f).roundToInt() // 少し上に表示

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            texture,
            textureX,
            textureY,
            0f,
            0f,
            displaySize,
            displaySize,
            displaySize,
            displaySize,
            TEXTURE_SIZE,
            TEXTURE_SIZE,
        )

        val titleAlpha = (255 * alpha).roundToInt()
        val title = Text.literal("Infinite Client")
        context.drawCenteredTextWithShadow(
            textRenderer,
            title,
            width / 2,
            textureY + displaySize + 12, // アニメーション画像の下に表示
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
            textureY + displaySize + 26, // タイトルの下に表示
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
}
