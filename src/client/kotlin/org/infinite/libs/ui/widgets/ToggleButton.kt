package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.mix
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
        tb.animationStartTime = System.currentTimeMillis()
    },
    DEFAULT_NARRATION,
) {
    protected abstract var value: Boolean
    private var animationStartTime: Long = -1L
    private val animationDuration = 200L
    private var beforeValue: Boolean? = null

    private fun renewCheck() {
        if (beforeValue == null) {
            beforeValue = value
        } else if (beforeValue != value) {
            beforeValue = value
            animationStartTime = System.currentTimeMillis()
        }
    }

    fun render(graphics2D: Graphics2D) {
        renewCheck()
        val colorScheme = InfiniteClient.theme.colorScheme

        // --- レイアウト計算 ---
        val margin = height * 0.15f
        val knobRadius = (height - (margin * 2)) / 2f
        val toggleAreaWidth = width.toFloat().coerceAtMost(height * 2f)
        val toggleAreaX = x + (width - toggleAreaWidth) / 2f

        // 背景バーを少しスリムに、かつ角を完全に丸く (height / 2)
        val barHeight = height * 0.6f
        val barY = y + (height - barHeight) / 2f
        val barCornerRadius = barHeight / 2f

        // --- アニメーション進捗 ---
        val currentTime = System.currentTimeMillis()
        val rawProgress = if (animationStartTime == -1L) {
            1f
        } else {
            ((currentTime - animationStartTime).toFloat() / animationDuration).coerceIn(0f, 1f)
        }

        // イージングを適用
        val easedProgress = sin(rawProgress * Math.PI / 2).toFloat()
        val mixFactor = if (value) easedProgress else 1f - easedProgress

        // ノブの移動範囲
        val minKnobCenterX = toggleAreaX + margin + knobRadius
        val maxKnobCenterX = toggleAreaX + toggleAreaWidth - margin - knobRadius
        val currentKnobCenterX = minKnobCenterX + (maxKnobCenterX - minKnobCenterX) * mixFactor

        // --- 描画 ---

        // 1. 背景バーの描画 (角丸)
        val colorOff = if (isHovered) colorScheme.secondaryColor else colorScheme.backgroundColor
        val colorOn = if (isHovered) colorScheme.greenColor else colorScheme.accentColor

        graphics2D.fillStyle = if (active) colorOff.mix(colorOn, mixFactor) else colorScheme.backgroundColor
        graphics2D.fillRoundedRect(toggleAreaX, barY, toggleAreaWidth, barHeight, barCornerRadius)

        // 2. ノブの描画 (円形)
        val knobY = y + (height / 2f)
        val knobBorderWidth = height * 0.08f

        // 外枠の色補間
        val strokeColorOff = colorScheme.secondaryColor
        val strokeColorOn = colorScheme.accentColor

        // ノブ本体 (塗りつぶし)
        graphics2D.fillStyle = if (isHovered) colorScheme.accentColor else colorScheme.foregroundColor
        graphics2D.fillCircle(currentKnobCenterX, knobY, knobRadius)

        // ノブの枠線 (ストローク)
        graphics2D.strokeStyle.width = knobBorderWidth
        graphics2D.strokeStyle.color = strokeColorOff.mix(strokeColorOn, mixFactor)
        // strokeCircle を自作していない場合は、円のパスを作って strokePath()
        graphics2D.beginPath()
        graphics2D.arc(currentKnobCenterX, knobY, knobRadius, 0f, (Math.PI * 2).toFloat())
        graphics2D.strokePath()

        // アニメーション終了判定
        if (rawProgress >= 1f) animationStartTime = -1L
    }

    override fun renderContents(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)
        render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }
}
