package org.infinite.infinite.ui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import kotlin.math.PI

class FeatureResetButton(x: Int, y: Int, width: Int, height: Int, feature: Feature) :
    Button(
        x,
        y,
        width,
        height,
        Component.literal("Reset"),
        {
            val soundManager = Minecraft.getInstance().soundManager
            playButtonClickSound(soundManager)
            feature.reset()
        },
        DEFAULT_NARRATION,
    ) {
    override fun renderContents(
        guiGraphics: GuiGraphics,
        i: Int,
        j: Int,
        f: Float,
    ) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)
        render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }

    private fun Graphics2D.renderResetIcon(x: Int, y: Int, width: Int, height: Int) =
        this.renderResetIcon(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

    private fun Graphics2D.renderResetIcon(x: Float, y: Float, width: Float, height: Float) {
        // 1. 時間パラメータの取得 (0.0 ~ 1.0)
        val duration = 2000.0 // 2秒で1サイクル
        val t = (System.currentTimeMillis() % duration) / duration

        val colorScheme = InfiniteClient.theme.colorScheme
        val accentColor = colorScheme.accentColor // アクセントカラーを使用
        val centerX = x + width / 2f
        val centerY = y + height / 2f
        val radius = (width.coerceAtMost(height) / 2f) * 0.7f // 少し余白を持たせる
        val angle = 2.0 * PI * t
        val angleF = angle.toFloat()
        this.push()
        this.pop()
    }

    fun render(
        graphics2D: Graphics2D,
    ) {
        val theme = InfiniteClient.theme
        theme.renderBackGround(this.x, this.y, this.width, this.height, graphics2D, 0.8f)
        graphics2D.renderResetIcon(this.x, this.y, this.width, this.height)
    }
}
