package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
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

    // T型をDoubleに変換するロジック（描画・計算用）
    protected fun T.toDoubleValue(): Double = this.toDouble()

    // DoubleをT型に変換するロジック（値更新用）
    // 具体的な型変換（v.toInt() 等）は継承先（NumberPropertyWidget等）で実装させる
    protected abstract fun convertToType(v: Double): T

    private var isDragging = false

    private val progress: Double
        get() = (
            (value.toDoubleValue() - minValue.toDoubleValue()) /
                (maxValue.toDoubleValue() - minValue.toDoubleValue())
            ).coerceIn(0.0, 1.0)

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
        // --- レイアウト計算で定義した margin と knobSize を考慮 ---
        val margin = height * 0.15f
        val knobSize = height * 0.7f
        val trackWidth = width - (margin * 2) - knobSize

        // マウス位置から進捗率を計算（ノブの中心がマウスに来るように調整）
        val relativeMouseX = mouseX - (x + margin + knobSize / 2f)
        val nextProgress = (relativeMouseX / trackWidth).coerceIn(0.0, 1.0)

        // 進捗から実数値を計算し、抽象メソッド経由で型変換して代入
        val nextDoubleValue =
            minValue.toDoubleValue() + (maxValue.toDoubleValue() - minValue.toDoubleValue()) * nextProgress
        value = convertToType(nextDoubleValue)
    }

    fun render(graphics2D: Graphics2D) {
        val colorScheme = InfiniteClient.theme.colorScheme

        val margin = height * 0.15f
        val barHeight = height * 0.2f
        val barY = y + (height - barHeight) / 2f
        val knobSize = height * 0.7f
        val knobY = y + (height - knobSize) / 2f

        val currentProgress = progress.toFloat()
        val trackWidth = width - (margin * 2) - knobSize
        val currentKnobX = (x + margin) + trackWidth * currentProgress

        // --- 背景バーの描画 ---
        val barStartX = x + margin
        val barEndX = x + width - margin
        val knobCenter = currentKnobX + (knobSize / 2f)

        graphics2D.fillStyle = if (isHovered || isDragging) colorScheme.accentColor else colorScheme.secondaryColor
        graphics2D.fillRect(barStartX, barY, knobCenter - barStartX, barHeight)

        graphics2D.fillStyle = colorScheme.backgroundColor
        graphics2D.fillRect(knobCenter, barY, barEndX - knobCenter, barHeight)

        // --- ノブの描画 ---
        val knobBorder = knobSize * 0.15f
        val mixFactor = if (isDragging) {
            1f
        } else if (isHovered) {
            0.5f
        } else {
            0f
        }
        val currentKnobColor = colorScheme.foregroundColor.mix(colorScheme.accentColor, mixFactor)
        val currentStrokeColor = colorScheme.secondaryColor.mix(colorScheme.accentColor, mixFactor)

        graphics2D.strokeStyle.width = knobBorder
        graphics2D.strokeStyle.color = currentStrokeColor
        graphics2D.strokeRect(currentKnobX, knobY, knobSize, knobSize)

        graphics2D.fillStyle = currentKnobColor
        graphics2D.fillRect(
            currentKnobX + knobBorder / 2f,
            knobY + knobBorder / 2f,
            knobSize - knobBorder,
            knobSize - knobBorder,
        )
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
