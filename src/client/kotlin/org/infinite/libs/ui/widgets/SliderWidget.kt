package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
import org.infinite.utils.mix

abstract class SliderWidget<T>(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) : AbstractWidget(x, y, width, height, Component.empty()) where T : Number, T : Comparable<T> {

    protected abstract val minValue: T
    protected abstract val maxValue: T
    protected abstract var value: T

    protected fun T.toDoubleValue(): Double = this.toDouble()
    protected abstract fun convertToType(v: Double): T

    private var isDragging = false

    // テーマと共通のアニメーション用に進捗を管理（任意でLerpを挟むとより滑らかになります）
    private val progress: Double
        get() = (
            (value.toDoubleValue() - minValue.toDoubleValue()) /
                (maxValue.toDoubleValue() - minValue.toDoubleValue())
            ).coerceIn(0.0, 1.0)

    private val themeScheme get() = InfiniteClient.theme.colorScheme

    override fun onClick(mouseButtonEvent: MouseButtonEvent, bl: Boolean) {
        isDragging = true
        updateValueFromMouse(mouseButtonEvent.x)
    }

    override fun onRelease(mouseButtonEvent: MouseButtonEvent) {
        isDragging = false
    }

    override fun onDrag(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double) {
        if (isDragging) {
            updateValueFromMouse(mouseButtonEvent.x)
        }
    }

    private fun updateValueFromMouse(mouseX: Double) {
        val trackX = x + 4f
        val trackWidth = width - 8f
        val nextProgress = ((mouseX - trackX) / trackWidth).coerceIn(0.0, 1.0)
        val nextDoubleValue =
            minValue.toDoubleValue() + (maxValue.toDoubleValue() - minValue.toDoubleValue()) * nextProgress
        value = convertToType(nextDoubleValue)
    }

    fun render(graphics2D: Graphics2D) {
        val trackX = x + 2f
        val trackY = y + height / 2f - 2f
        val trackW = width - 4f
        val trackH = 4f
        val trackRadius = trackH / 2f

        val currentProgress = progress.toFloat()
        val knobX = trackX + (trackW * currentProgress)
        val knobY = y + height / 2f
        val knobRadius = (
            if (isDragging) {
                6f
            } else if (isHovered) {
                5f
            } else {
                4f
            }
            )

        // --- 1. トラック（背景）の描画 ---
        // 背景より少し明るい色で溝を表現
        graphics2D.fillStyle = themeScheme.surfaceColor.mix(themeScheme.backgroundColor, 0.5f)
        graphics2D.fillRoundedRect(trackX, trackY, trackW, trackH, trackRadius)

        // --- 2. 塗りつぶし部分（アクセント色） ---
        // 有効ならアクセントカラー、無効ならセカンダリカラー
        val barColor = if (active) themeScheme.accentColor else themeScheme.secondaryColor
        graphics2D.fillStyle = barColor.alpha(if (isDragging) 255 else 200)
        graphics2D.fillRoundedRect(trackX, trackY, trackW * currentProgress, trackH, trackRadius)

        // --- 3. ノブ（つまみ）とグローエフェクト ---
        val hoverAlpha = if (isDragging) {
            1.0f
        } else if (isHovered) {
            0.6f
        } else {
            0.0f
        }

        graphics2D.fillStyle = barColor.alpha((60 * hoverAlpha).toInt())
        graphics2D.fillCircle(knobX, knobY, knobRadius + 3f)

        // ノブ本体
        // ドラッグ中はアクセントカラー、それ以外は文字色に近い色で描画
        val knobColor = if (isDragging) {
            themeScheme.accentColor
        } else {
            themeScheme.foregroundColor.mix(themeScheme.accentColor, hoverAlpha * 0.3f)
        }

        graphics2D.fillStyle = knobColor
        graphics2D.fillCircle(knobX, knobY, knobRadius)

        // ノブの縁取り
        graphics2D.strokeStyle.apply {
            width = 1f
            color = themeScheme.backgroundColor.alpha(100)
        }
        graphics2D.strokeCircle(knobX, knobY, knobRadius)
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val renderer = Graphics2DRenderer(guiGraphics)
        render(renderer)
        renderer.flush()
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        defaultButtonNarrationText(output)
    }
}
