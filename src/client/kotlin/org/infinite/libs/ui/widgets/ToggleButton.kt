package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.ClickGuiPalette
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.mix
import kotlin.math.PI
import kotlin.math.sin

abstract class ToggleButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) : Button(
    x,
    y,
    width,
    height,
    Component.empty(),
    { button ->
        val tb = button as ToggleButton
        tb.value = !tb.value
    },
    DEFAULT_NARRATION,
) {
    protected abstract var value: Boolean

    // アニメーション用変数
    private var animationStartTime: Long = -1L
    private val animationDuration = 200L
    private var lastValue: Boolean? = null

    /**
     * 値の変化を検知してアニメーションを開始する
     */
    private fun updateAnimation() {
        if (lastValue == null) {
            lastValue = value
        } else if (lastValue != value) {
            lastValue = value
            animationStartTime = System.currentTimeMillis()
        }
    }

    fun render(graphics2D: Graphics2D) {
        updateAnimation() // アニメーションチェック

        // --- レイアウト計算 ---
        val margin = height * 0.15f
        val knobRadius = (height - (margin * 2)) / 2f

        // トグルエリアの幅を制限して中央に配置
        val toggleAreaWidth = width.toFloat().coerceAtMost(height * 2f)
        val toggleAreaX = x + (width - toggleAreaWidth) / 2f

        // 背景バーを少しスリムに
        val barHeight = height * 0.65f
        val barY = y + (height - barHeight) / 2f
        val barCornerRadius = barHeight / 2f

        // --- アニメーション進捗計算 ---
        val currentTime = System.currentTimeMillis()
        val rawProgress = if (animationStartTime == -1L) {
            1f
        } else {
            ((currentTime - animationStartTime).toFloat() / animationDuration).coerceIn(0f, 1f)
        }

        // イージング（サイン波）を適用して滑らかに
        val easedProgress = sin(rawProgress * PI / 2).toFloat()
        val mixFactor = if (value) easedProgress else 1f - easedProgress

        // ノブの水平移動範囲
        val minKnobCenterX = toggleAreaX + margin + knobRadius
        val maxKnobCenterX = toggleAreaX + toggleAreaWidth - margin - knobRadius
        val currentKnobCenterX = minKnobCenterX + (maxKnobCenterX - minKnobCenterX) * mixFactor

        // --- 描画 ---

        // 1. 背景バーの描画
        val colorOff = ClickGuiPalette.PANEL_ALT
        val colorOn = ClickGuiPalette.ACCENT

        graphics2D.fillStyle = if (active) colorOff.mix(colorOn, mixFactor) else colorOff
        if (isHovered) {
            // ホバー時は少し明るく
            graphics2D.fillStyle = graphics2D.fillStyle.mix(ClickGuiPalette.HOVER, 0.2f)
        }
        graphics2D.fillRoundedRect(toggleAreaX, barY, toggleAreaWidth, barHeight, barCornerRadius)

        // 2. ノブ（円形）の描画
        val knobCenterY = y + (height / 2f)

        // ノブ本体
        graphics2D.fillStyle = if (value) ClickGuiPalette.TEXT else ClickGuiPalette.MUTED
        graphics2D.fillCircle(currentKnobCenterX, knobCenterY, knobRadius)

        // ノブの枠線 (高級感を出すために追加)
        graphics2D.strokeStyle.width = 1.0f
        graphics2D.strokeStyle.color = if (value) ClickGuiPalette.ACCENT_DARK else ClickGuiPalette.BORDER
        graphics2D.strokeCircle(currentKnobCenterX, knobCenterY, knobRadius)

        // アニメーション終了判定
        if (rawProgress >= 1f) animationStartTime = -1L
    }

    override fun renderContents(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)
        render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }
}
