package org.infinite.infinite.features.global.rendering.loading

import net.minecraft.client.gui.GuiGraphics
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

    // スプライトシートを保持（512x256 の画像が 20x18 枚並んでいる前提）
    private val spriteSheet = Image("infinite:textures/gui/loading_animations/animation.png")

    private var frame: Int = 0
        set(value) {
            field = value % totalFrame
        }

    private val totalFrame = 360
    private val columns = 20

    // 1コマあたりのソース画像サイズ
    private val frameSrcWidth = 512
    private val frameSrcHeight = 256

    fun handleRender(ctx: LoadingRenderContext) {
        val g2d = Graphics2DRenderer(ctx.guiGraphics)

        // 背景描画
        g2d.fillStyle = 0xFF000000.toInt()
        g2d.fillRect(0, 0, g2d.width, g2d.height)

        val alphaInt = (ctx.fadeOpacity * 255).toInt()
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme

        // テーマ固有の背景装飾
        val bgAlpha = ctx.fadeOpacity * 0.25f
        theme.renderBackGround(0, 0, g2d.width, g2d.height, g2d, bgAlpha)

        val centerX = ctx.centerX.toFloat()
        val centerY = ctx.centerY.toFloat()
        val diffY = 30f

        // --- スプライトシートのピクセル座標計算 ---
        val col = frame % columns
        val row = frame / columns

        // UVをピクセル単位で指定 (Int)
        val u = col * frameSrcWidth
        val v = row * frameSrcHeight

        // 表示サイズ（2:1のアスペクト比を維持）
        val drawWidth = 60f
        val drawHeight = drawWidth / 2f

        // フェード不透明度を色に適用
        val renderColor = 0xFFFFFF.alpha(alphaInt)

        // imageCenteredをラップした内部的なUV対応版、
        // あるいは image メソッドを直接呼び出して中心座標を調整
        // image メソッドは引数 u, v, uw, vh に Int を取るため、計算したピクセル値を渡す
        g2d.image(
            image = spriteSheet,
            x = centerX - drawWidth / 2f,
            y = centerY - diffY - drawHeight / 2f,
            w = drawWidth,
            h = drawHeight,
            u = u,
            v = v,
            uw = frameSrcWidth,
            vh = frameSrcHeight,
            color = renderColor,
        )

        // --- 無限マークの軌道プログレス演出 ---
        val radius = 55f
        val frameProgress = frame.toFloat() / totalFrame.toFloat()

        val startAngle = frameProgress * 2f * PI.toFloat()
        val sweepAngle = (PI.toFloat() * 2f * ctx.progress).coerceAtLeast(0.05f)

        g2d.beginPath()
        g2d.strokeStyle.color = colorScheme.accentColor.alpha(alphaInt)
        g2d.strokeStyle.width = 4f
        g2d.arc(centerX, centerY - diffY, radius, startAngle, startAngle + sweepAngle)
        g2d.strokePath()

        g2d.flush()

        frame++
    }
}
