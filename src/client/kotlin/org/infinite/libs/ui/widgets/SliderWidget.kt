package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.ClickGuiPalette
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.fillRoundedRect

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
        val trackX = x + 4f
        val trackWidth = width - 8f
        val nextProgress = ((mouseX - trackX) / trackWidth).coerceIn(0.0, 1.0)

        // 進捗から実数値を計算し、抽象メソッド経由で型変換して代入
        val nextDoubleValue =
            minValue.toDoubleValue() + (maxValue.toDoubleValue() - minValue.toDoubleValue()) * nextProgress
        value = convertToType(nextDoubleValue)
    }

    fun render(graphics2D: Graphics2D) {
        val trackX = x + 4f
        val trackY = y + height / 2f - 3f
        val trackW = width - 8f
        val trackH = 6f
        val trackRadius = trackH / 2f

        val currentProgress = progress.toFloat()
        val knobSize = 10f
        val knobX = trackX + (trackW * currentProgress) - knobSize / 2f
        val knobY = y + (height - knobSize) / 2f
        val knobRadius = knobSize / 2f

        graphics2D.fillStyle = ClickGuiPalette.PANEL_ALT
        graphics2D.fillRoundedRect(trackX, trackY, trackW, trackH, trackRadius)

        graphics2D.fillStyle = if (active) ClickGuiPalette.ACCENT else ClickGuiPalette.BORDER
        graphics2D.fillRoundedRect(trackX, trackY, trackW * currentProgress, trackH, trackRadius)

        graphics2D.fillStyle = ClickGuiPalette.TEXT
        graphics2D.fillRoundedRect(knobX, knobY, knobSize, knobSize, knobRadius)
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val renderer = Graphics2DRenderer(guiGraphics)
        render(renderer)
        renderer.flush()
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)
}
