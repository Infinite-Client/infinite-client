package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.ClickGuiPalette
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

        // --- トラック（背景）の描画 ---
        graphics2D.fillStyle = ClickGuiPalette.PANEL_ALT
        graphics2D.fillRoundedRect(trackX, trackY, trackW, trackH, trackRadius)

        // --- 進捗バー（アクセント色）の描画 ---
        // active状態やドラッグ状態に応じて色を変えるとより直感的になります
        graphics2D.fillStyle = if (active) ClickGuiPalette.ACCENT else ClickGuiPalette.BORDER
        graphics2D.fillRoundedRect(trackX, trackY, trackW * currentProgress, trackH, trackRadius)

        // --- ノブ（つまみ）の描画 ---
        // HEAD側の「状態に応じた色の変化」を、dev側の「丸ノブ」に適用します
        val mixFactor = if (isDragging) {
            1.0f
        } else if (isMouseOver(x.toDouble(), y.toDouble())) { // 擬似的にisHoveredを判定
            0.5f
        } else {
            0.0f
        }

        // ClickGuiPaletteとHEAD側の色の変化ロジックを融合
        val knobColor = ClickGuiPalette.TEXT.mix(ClickGuiPalette.ACCENT, mixFactor)

        graphics2D.fillStyle = knobColor
        graphics2D.fillCircle(knobX + knobRadius, knobY + knobRadius, knobRadius)

        // ドラッグ中やホバー中に少し外枠（グロー）を付けると高級感が出ます
        if (mixFactor > 0f) {
            graphics2D.strokeStyle.width = 1.5f
            graphics2D.strokeStyle.color = ClickGuiPalette.ACCENT.alpha((100 * mixFactor).toInt())
            graphics2D.strokeCircle(knobX + knobRadius, knobY + knobRadius, knobRadius + 1f)
        }
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
