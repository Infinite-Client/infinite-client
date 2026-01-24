package org.infinite.infinite.features.global.rendering.loading

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.Identifier
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.GlobalFeature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.graphics.graphics2d.structs.Image
import org.infinite.utils.alpha
import kotlin.math.PI

class InfiniteLoadingFeature : GlobalFeature() {
    data class LoadingRenderContext(
        val guiGraphics: GuiGraphics,
        val mouseX: Int,
        val mouseY: Int,
        val partialTick: Float,
        val fadeOpacity: Float,
        val progress: Float,
        val centerX: Int,
        val centerY: Int,
        val logoWidth: Int,
        val logoHeight: Int,
    )

    private var frame: Int = 0
        set(value) {
            field = value % totalFrame
        }
    private val totalFrame = 360
    private val frameProgress: Float
        get() = frame.toFloat() / totalFrame.toFloat()

    fun handleRender(ctx: LoadingRenderContext) {
        val g2d = Graphics2DRenderer(ctx.guiGraphics, Minecraft.getInstance().deltaTracker)

        // 背景塗りつぶし
        g2d.fillStyle = 0xFF000000.toInt()
        g2d.fillRect(0, 0, g2d.width, g2d.height)

        val alphaInt = (ctx.fadeOpacity * 255).toInt()
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme

        // 背景レンダリング
        val bgAlpha = ctx.fadeOpacity * 0.25f
        theme.renderBackGround(0, 0, g2d.width, g2d.height, g2d, bgAlpha)

        // --- フレーム番号の4桁ゼロ埋め処理 ---
        // 例: frameが5なら "0005" になる
        val frameString = frame.toString().padStart(4, '0')
        val imagePath = "infinite:textures/gui/loading_animations/animation-$frameString.png"
        val image = Image(Identifier.parse(imagePath))
        val centerX = ctx.centerX.toFloat()
        val centerY = ctx.centerY.toFloat()
        val radius = 40f
        val size = radius * 1.5f
        val diffY = 30f
        val imageDiff = size / 4f
        g2d.imageCentered(image, centerX, centerY - diffY + imageDiff, size, size)

        // 円形のプログレスバー
        val startAngle = frameProgress * 2f * PI.toFloat()
        val endAngle = startAngle + (PI.toFloat() * 2f * ctx.progress)
        g2d.beginPath()
        g2d.strokeStyle.color = colorScheme.accentColor.alpha(alphaInt)
        g2d.strokeStyle.width = 4f
        g2d.arc(centerX, centerY - diffY, radius, startAngle, endAngle)
        g2d.strokePath()
        g2d.flush()
        frame++
    }
}
