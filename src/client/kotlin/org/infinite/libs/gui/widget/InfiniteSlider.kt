package org.infinite.libs.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.settings.FeatureSetting

class InfiniteSlider<T : Number>(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val setting: FeatureSetting<T>,
) : AbstractWidget(x, y, width, height, Component.literal(setting.name)) {
    private val textRenderer = Minecraft.getInstance().font
    private var dragging = false
    private val knobWidth = 4

    init {
        updateMessage()
        setting.addChangeListener {
            updateMessage()
        }
    }

    private fun updateMessage() {
        val formattedValue =
            when (setting) {
                is FeatureSetting.IntSetting -> setting.value.toString()
                is FeatureSetting.FloatSetting -> String.format("%.2f", setting.value)
                is FeatureSetting.DoubleSetting -> String.format("%.3f", setting.value)
                else -> throw IllegalStateException("InfiniteSlider can only be used with IntSetting or FloatSetting")
            }
        message = Component.translatable(setting.name).append(": $formattedValue")
    }

    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)

        val textX = x + 5 // Padding from left edge
        var currentY = y + 2 // Start drawing text from top with small padding

        // --- 設定名 (左上) ---
        graphics2D.drawText(
            Component.translatable(setting.name),
            textX,
            currentY,
            InfiniteClient
                .currentColors()
                .foregroundColor,
            true, // shadow = true
        )

        // --- 値の描画 (右上) ---
        val formattedValue =
            when (setting) {
                is FeatureSetting.IntSetting -> setting.value.toString()
                is FeatureSetting.FloatSetting -> String.format("%.2f", setting.value)
                is FeatureSetting.DoubleSetting -> String.format("%.3f", setting.value)
                else -> "" // 通常は発生しない
            }
        val valueText = Component.literal(formattedValue)
        val valueTextWidth = textRenderer.width(valueText)
        val valueTextX = x + width - 5 - valueTextWidth // 右端から5pxパディング

        graphics2D.drawText(
            valueText,
            valueTextX,
            currentY, // 設定名と同じY座標
            InfiniteClient
                .currentColors()
                .primaryColor, // 例としてプライマリカラーを使用
            true, // shadow = true
        )
        // -----------------------

        currentY += textRenderer.lineHeight + 2 // Move Y down for description

        if (setting.descriptionKey.isNotBlank()) {
            graphics2D.drawText(
                Component.translatable(setting.descriptionKey),
                textX,
                currentY,
                InfiniteClient
                    .currentColors()
                    .secondaryColor,
                true, // shadow = true
            )
            // textRenderer.fontHeight + 2 // Move Y down after description - この行はコメントアウトまたは削除
        }

        // Draw slider background at the bottom of the widget
        val sliderBackgroundY = y + height - 5
        context.fill(
            x + 5,
            sliderBackgroundY,
            x + width - 5,
            sliderBackgroundY + 2,
            InfiniteClient
                .currentColors()
                .secondaryColor,
        )

        // Draw slider knob
        val progress = getProgress()
        val knobPositionRange = width - 10 - knobWidth
        val knobX = x + 5 + (knobPositionRange) * progress
        val knobY = y + height - 7 // Knob is 6px tall, so its top is 7px from bottom

        context.fill(
            knobX.toInt(),
            knobY,
            knobX.toInt() + knobWidth,
            knobY + 6,
            InfiniteClient
                .currentColors()
                .primaryColor,
        )
    }

    private fun getProgress(): Float =
        when (setting) {
            is FeatureSetting.IntSetting -> {
                val range = (setting.max - setting.min).toFloat()
                (setting.value.toFloat() - setting.min) / range
            }

            is FeatureSetting.FloatSetting -> {
                val range = (setting.max - setting.min)
                (setting.value - setting.min) / range
            }

            is FeatureSetting.DoubleSetting -> {
                val range = (setting.max - setting.min).toFloat()
                (setting.value - setting.min).toFloat() / range
            }

            else -> {
                throw IllegalStateException("InfiniteSlider can only be used with IntSetting or FloatSetting")
            }
        }

    private fun setValueFromMouse(mouseX: Double) {
        // スライダーの有効範囲 (ノブの左端が動ける範囲)
        val minX = x + 5.0
        val maxX = x + width - 5.0 - knobWidth

        // マウス位置をスライダー範囲にクランプ
        val clampedMouseX = Mth.clamp(mouseX, minX, maxX)

        // クランプされたX位置から進行度 (0.0F〜1.0F) を計算
        val progress = ((clampedMouseX - minX) / (maxX - minX)).toFloat()

        when (setting) {
            is FeatureSetting.IntSetting -> {
                // 新しい値を計算し、Intに丸める
                val newValue = (setting.min + progress * (setting.max - setting.min)).toInt()
                (setting as FeatureSetting.IntSetting).value = newValue
            }

            is FeatureSetting.FloatSetting -> {
                // 新しい値を計算
                val newValue = setting.min + progress * (setting.max - setting.min)
                (setting as FeatureSetting.FloatSetting).value = newValue
            }

            is FeatureSetting.DoubleSetting -> {
                val newValue = setting.min + progress * (setting.max - setting.min)
                (setting as FeatureSetting.DoubleSetting).value = newValue
            }

            else -> {
                throw IllegalStateException("InfiniteSlider can only be used with IntSetting or FloatSetting")
            }
        }
        updateMessage()
    }

    /**
     * マウスがクリックされたとき、ドラッグ状態を開始し、初期値を設定します。
     */
    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        if (isMouseOver(click.x, click.y) && click.button() == 0) {
            dragging = true
            // 値を設定し、mouseDraggedで継続して処理されるように true を返す
            setValueFromMouse(click.x)
            return true
        }
        return false
    }

    /**
     * マウスがドラッグされているとき、スライダーの値を更新します。
     */
    override fun mouseDragged(
        click: MouseButtonEvent,
        offsetX: Double,
        offsetY: Double,
    ): Boolean {
        if (dragging) {
            setValueFromMouse(click.x)
            return true
        }
        return false
    }

    /**
     * マウスがリリースされたとき、ドラッグ状態を終了します。
     */
    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        if (dragging) {
            dragging = false
            return true // ドラッグを終了したので true を返す
        }
        return super.mouseReleased(click)
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        this.defaultButtonNarrationText(builder)
    }
}
