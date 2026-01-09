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
        // 初回（nullの場合）または値が変化した瞬間を検知
        if (beforeValue == null) {
            beforeValue = value
            // 初回はアニメーションさせないならStartTimeは更新しない
        } else if (beforeValue != value) {
            beforeValue = value
            animationStartTime = System.currentTimeMillis()
        }
    }

    fun render(graphics2D: Graphics2D) {
        renewCheck()
        val colorScheme = InfiniteClient.theme.colorScheme

        // --- サイズ計算 ---
        val margin = height * 0.1f
        val knobSize = height - (margin * 2)
        val toggleAreaWidth = width.toFloat().coerceAtMost(height * 2f)
        val toggleAreaX = x + (width - toggleAreaWidth) / 2f
        val barHeight = height * 0.4f
        val barY = y + (height - barHeight) / 2f
        val barWidth = toggleAreaWidth - margin

        // --- アニメーション・進捗計算 ---
        val currentTime = System.currentTimeMillis()
        val rawProgress = if (animationStartTime == -1L) {
            1f
        } else {
            ((currentTime - animationStartTime).toFloat() / animationDuration).coerceIn(0f, 1f)
        }

        // サインカーブによるイージング (0.0 -> 1.0)
        val easedProgress = sin(rawProgress * Math.PI / 2).toFloat()

        // 移動位置の計算 (valueがtrueに向かっているなら 0->1、falseに向かっているなら 1->0 になるよう調整)
        // 現在の value に基づいて、補間係数 (mixFactor) を決定
        val mixFactor = if (value) easedProgress else 1f - easedProgress

        val minKnobX = toggleAreaX + margin
        val maxKnobX = toggleAreaX + toggleAreaWidth - knobSize - margin
        val currentKnobX = minKnobX + (maxKnobX - minKnobX) * mixFactor

        // --- 色の計算 (mix関数の活用) ---
        // 状態ごとの色を定義
        val colorOff = if (isHovered) colorScheme.secondaryColor else colorScheme.backgroundColor
        val colorOn = if (isHovered) colorScheme.greenColor else colorScheme.accentColor

        // 背景バーの色をミックス
        val currentBarColor = colorOff.mix(colorOn, mixFactor)

        // 背景バーの描画
        graphics2D.fillStyle = if (active) currentBarColor else colorScheme.backgroundColor
        graphics2D.fillRect(toggleAreaX + (margin / 2), barY, barWidth, barHeight)

        // --- ノブの描画 ---
        val knobY = y + margin
        val knobBorder = knobSize * 0.15f

        // ノブの枠線の色をミックス
        val strokeColorOff = colorScheme.secondaryColor
        val strokeColorOn = colorScheme.accentColor
        val currentStrokeColor = strokeColorOff.mix(strokeColorOn, mixFactor)

        // 内側ノブ
        graphics2D.fillStyle = if (isHovered) colorScheme.accentColor else colorScheme.foregroundColor
        graphics2D.fillRect(
            currentKnobX + knobBorder / 2f,
            knobY + knobBorder / 2f,
            knobSize - knobBorder, // サイズ計算の微調整
            knobSize - knobBorder,
        )

        // 外枠ノブ
        graphics2D.strokeStyle.width = knobBorder
        graphics2D.strokeStyle.color = currentStrokeColor
        graphics2D.strokeRect(currentKnobX, knobY, knobSize, knobSize)

        // アニメーション終了判定
        if (rawProgress >= 1f) {
            animationStartTime = -1L
        }
    }

    override fun renderContents(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)
        render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }
}
