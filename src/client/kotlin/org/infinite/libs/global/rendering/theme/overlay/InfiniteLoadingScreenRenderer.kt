package org.infinite.libs.global.rendering.theme.overlay

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
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
            Identifier.fromNamespaceAndPath("infinite", "textures/gui/loading_animations/animation-${String.format("%04d", i)}.png")
        }
    }

    @JvmStatic
    fun render(
        context: GuiGraphics,
        progress: Float,
        reloadStartTime: Long,
        reloadCompleteTime: Long,
        reloading: Boolean,
    ) {
        val g2d = Graphics2D(context) // Graphics2Dインスタンスを作成

        val now = System.currentTimeMillis()
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

        val width = g2d.width // Graphics2Dから取得
        val height = g2d.height // Graphics2Dから取得

        // 1. 背景のグラデーション塗りつぶし
        // Graphics2DにはfillGradientがないため、DrawContextのメソッドを直接使用するか、
        // Graphics2D内にfillGradientを実装する必要があります。ここではDrawContextをラップしているため、直接使用します。
        val topColor = ARGB.color((255 * alpha).roundToInt(), 8, 10, 14)
        val bottomColor = ARGB.color((255 * alpha).roundToInt(), 0, 0, 0)
        context.fillGradient(0, 0, width, height, topColor, bottomColor)

        // 2. アニメーション画像の描画
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
            rotation = 0f, // 回転なし
            color = ARGB.color((255 * alpha).roundToInt(), 255, 255, 255), // アルファ値を適用
            u = 0f,
            v = 0f,
            uWidth = TEXTURE_SIZE.toFloat(),
            vHeight = TEXTURE_SIZE.toFloat(),
            textureWidth = TEXTURE_SIZE.toFloat(),
            textureHeight = TEXTURE_SIZE.toFloat(),
        )
        val fontHeight = g2d.fontHeight()
        // 3. タイトルテキストの描画
        val titleAlpha = ARGB.color((255 * alpha).roundToInt(), 255, 255, 255)
        val title = Component.literal("Infinite Client")
        g2d.centeredText(
            text = title,
            x = width / 2,
            y = textureY + displaySize + 3 * fontHeight,
            color = titleAlpha,
            shadow = true,
        )

        // 4. サブタイトルテキストの描画 (Y座標を +26 から +38 に変更)
        val subtitleText =
            if (reloading) {
                Component.literal("Loading resources...")
            } else {
                Component.literal("Finishing setup...")
            }
        val subtitleAlpha = ARGB.color((210 * alpha).roundToInt(), 180, 180, 180)
        g2d.centeredText(
            text = subtitleText,
            x = width / 2,
            y = textureY + displaySize + 4 * fontHeight,
            color = subtitleAlpha,
            shadow = true,
        )

        // 5. プログレスバーの描画
        val progressBarWidth = (width * 0.55f).roundToInt()
        val progressBarX = (width - progressBarWidth) / 2
        val progressBarY = (height * 0.68f).roundToInt()

        // ベースバーの描画 (rect)
        val baseBarColor = ARGB.color((80 * alpha).roundToInt(), 255, 255, 255)
        g2d.rect(
            x1 = progressBarX.toFloat(),
            y1 = progressBarY.toFloat(),
            x2 = (progressBarX + progressBarWidth).toFloat(),
            y2 = (progressBarY + fontHeight).toFloat(),
            color = baseBarColor,
        )

        // フィル部分の描画 (rect)
        val clampedProgress = progress.coerceIn(0f, 1f)
        val filledWidth = (progressBarWidth * clampedProgress).roundToInt()
        val fillColor = ARGB.color((230 * alpha).roundToInt(), 255, 255, 255)

        if (filledWidth > 0) {
            g2d.rect(
                x1 = progressBarX.toFloat(),
                y1 = progressBarY.toFloat(),
                x2 = (progressBarX + filledWidth).toFloat(),
                y2 = (progressBarY + fontHeight).toFloat(),
                color = fillColor,
            )
        }
        // 6. パーセンテージテキストの描画 (Y座標は変更なし)
        val percentText = "${(clampedProgress * 100f).roundToInt()}%"
        val percentTitle = Component.literal("$percentText • Infinite")
        val percentAlpha = ARGB.color((200 * alpha).roundToInt(), 210, 210, 210)

        g2d.centeredText(
            text = percentTitle,
            x = width / 2,
            y = progressBarY + 4 * fontHeight, // 変更なし
            color = percentAlpha,
            shadow = true,
        )
    }
}
