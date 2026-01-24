package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
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

    private var animationStartTime: Long = -1L
    private val animationDuration = 220L // わずかにゆったりさせて高級感を出す
    private var lastValue: Boolean? = null

    // テーマ取得
    private val themeScheme get() = InfiniteClient.theme.colorScheme

    private fun updateAnimation() {
        if (lastValue == null) {
            lastValue = value
        } else if (lastValue != value) {
            lastValue = value
            animationStartTime = System.currentTimeMillis()
        }
    }

    fun render(graphics2D: Graphics2D) {
        updateAnimation()

        // --- レイアウト計算 ---
        val margin = height * 0.18f
        val knobRadius = (height - (margin * 2)) / 2f

        // トグルの土台（カプセル形状）のサイズ
        val toggleAreaWidth = 34f // 固定幅にするとリスト内で揃って綺麗
        val toggleAreaX = x + width - toggleAreaWidth - 2f // 右寄せ
        val toggleAreaY = y + (height - (height * 0.8f)) / 2f
        val toggleH = height * 0.8f
        val cornerRadius = toggleH / 2f

        // --- アニメーション進捗 (0.0 -> 1.0) ---
        val currentTime = System.currentTimeMillis()
        val rawProgress = if (animationStartTime == -1L) {
            1f
        } else {
            ((currentTime - animationStartTime).toFloat() / animationDuration).coerceIn(0f, 1f)
        }

        // イージング適用
        val easedProgress = sin(rawProgress * PI / 2).toFloat()
        val mixFactor = if (value) easedProgress else 1f - easedProgress

        // --- 描画 ---

        // 1. 背景（ベース）
        // OFF: surfaceColor, ON: accentColor
        val colorOff = themeScheme.surfaceColor.mix(themeScheme.backgroundColor, 0.3f)
        val colorOn = themeScheme.accentColor

        var bg = colorOff.mix(colorOn, mixFactor)
        if (isHovered && active) bg = themeScheme.getHoverColor(bg)

        graphics2D.fillStyle = bg.alpha(if (active) 255 else 100)
        graphics2D.fillRoundedRect(toggleAreaX, toggleAreaY, toggleAreaWidth, toggleH, cornerRadius)

        // 2. ノブの移動計算
        val minX = toggleAreaX + margin + knobRadius
        val maxX = toggleAreaX + toggleAreaWidth - margin - knobRadius
        val currentKnobX = minX + (maxX - minX) * mixFactor
        val knobY = toggleAreaY + toggleH / 2f

        // ノブ本体 (常に明るい色)
        graphics2D.fillStyle = themeScheme.foregroundColor
        graphics2D.fillCircle(currentKnobX, knobY, knobRadius)

        graphics2D.strokeStyle.color = themeScheme.accentColor.alpha((60 * mixFactor).toInt())
        graphics2D.strokeStyle.width = 1f
        graphics2D.strokeCircle(currentKnobX, knobY, knobRadius)

        // ノブの縁取り
        graphics2D.strokeStyle.apply {
            width = 1f
            color = themeScheme.backgroundColor.alpha(80)
        }
        graphics2D.strokeCircle(currentKnobX, knobY, knobRadius)

        if (rawProgress >= 1f) animationStartTime = -1L
    }

    override fun renderContents(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)
        render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }
}
