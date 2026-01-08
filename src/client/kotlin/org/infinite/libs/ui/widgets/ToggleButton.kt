package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
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

    /**
     * Graphics2D を使用したカスタム描画ロジック
     */
    fun render(graphics2D: Graphics2D) {
        val colorScheme = InfiniteClient.theme.colorScheme

        // 1. 背景バーの描画設定
        val backgroundColor = when {
            !active -> colorScheme.backgroundColor
            value -> if (isHovered) colorScheme.greenColor else colorScheme.accentColor
            else -> if (isHovered) colorScheme.secondaryColor else colorScheme.backgroundColor
        }

        val knobSize = height - 4f
        val barWidth = knobSize * 2f
        val barHeight = height.toFloat() / 2.5f
        val barX = x + (width - barWidth) / 2f
        val barY = y + (height - barHeight) / 2f

        graphics2D.fillStyle = backgroundColor
        graphics2D.fillRect(barX, barY, barWidth, barHeight)

        // 2. アニメーション計算
        val startKnobX = if (!value) barX + barWidth - knobSize - 2f else barX + 2f
        val endKnobX = if (value) barX + barWidth - knobSize - 2f else barX + 2f

        val currentKnobX = if (animationStartTime == -1L) {
            endKnobX
        } else {
            val currentTime = System.currentTimeMillis()
            val animProgress = (currentTime - animationStartTime).toFloat() / animationDuration
            if (animProgress < 1.0f) {
                val easedProgress = sin(animProgress * Math.PI / 2).toFloat()
                startKnobX + (endKnobX - startKnobX) * easedProgress
            } else {
                animationStartTime = -1L
                endKnobX
            }
        }

        // 3. ノブの描画
        val knobY = y + 2f
        val knobBorder = 2f

        // 外枠ノブ
        graphics2D.fillStyle = if (active) colorScheme.accentColor else colorScheme.backgroundColor
        graphics2D.fillRect(currentKnobX, knobY, knobSize, knobSize)

        // 内側ノブ
        graphics2D.fillStyle = if (isHovered) colorScheme.accentColor else colorScheme.foregroundColor
        graphics2D.fillRect(
            currentKnobX + knobBorder,
            knobY + knobBorder,
            knobSize - (knobBorder * 2),
            knobSize - (knobBorder * 2),
        )
    }

    override fun renderContents(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)
        render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }
}
