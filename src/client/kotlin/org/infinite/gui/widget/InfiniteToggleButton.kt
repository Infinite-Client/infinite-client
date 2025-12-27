package org.infinite.gui.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.text.Text
import org.infinite.InfiniteClient
import kotlin.math.sin

class InfiniteToggleButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private var state: Boolean,
    private var isEnabled: Boolean,
    private val onToggle: (Boolean) -> Unit,
) : ClickableWidget(x, y, width, height, Text.empty()) {
    private var animationStartTime: Long = -1L
    private val animationDuration = 200L

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val colors = InfiniteClient.currentColors()
        val interpolatedColor = colors.primaryColor

        val backgroundColor =
            when {
                !isEnabled -> colors.backgroundColor
                state -> if (isHovered) colors.greenAccentColor else colors.primaryColor
                else -> if (isHovered) colors.secondaryColor else colors.backgroundColor
            }

        val knobSize = height - 4
        val barWidth = (knobSize * 2).toFloat()
        val barHeight = height.toFloat() / 2.5f
        val barY = y + (height - barHeight.toInt()) / 2
        val barX = x + (width - barWidth.toInt()) / 2

        context.fill(barX, barY, (barX + barWidth).toInt(), (barY + barHeight).toInt(), backgroundColor)

        val knobBorder = 2
        val startKnobX = if (!state) barX + barWidth.toInt() - knobSize - 2 else barX + 2
        val endKnobX = if (state) barX + barWidth.toInt() - knobSize - 2 else barX + 2

        var currentKnobX = endKnobX.toFloat()
        if (animationStartTime != -1L) {
            val currentTime = System.currentTimeMillis()
            val animProgress = (currentTime - animationStartTime).toFloat() / animationDuration.toFloat()
            if (animProgress < 1.0f) {
                val easedProgress = sin(animProgress * Math.PI / 2).toFloat()
                currentKnobX = startKnobX + (endKnobX - startKnobX) * easedProgress
            } else {
                animationStartTime = -1L
                currentKnobX = endKnobX.toFloat()
            }
        }
        val knobY = y + 2

        val knobBorderColor = if (isEnabled) interpolatedColor else colors.backgroundColor
        context.fill(currentKnobX.toInt(), knobY, currentKnobX.toInt() + knobSize, knobY + knobSize, knobBorderColor)

        val knobInnerColor = if (isHovered) colors.primaryColor else colors.foregroundColor
        context.fill(
            currentKnobX.toInt() + knobBorder,
            knobY + knobBorder,
            currentKnobX.toInt() + knobSize - knobBorder,
            knobY + knobSize - knobBorder,
            knobInnerColor,
        )
    }

    override fun mouseClicked(
        click: Click,
        doubled: Boolean,
    ): Boolean {
        if (!isMouseOver(click.x, click.y) || !active || !isEnabled) return false
        playDownSound(MinecraftClient.getInstance().soundManager)
        state = !state
        onToggle(state)
        animationStartTime = System.currentTimeMillis()
        return true
    }

    fun setState(newState: Boolean) {
        state = newState
    }

    fun press() {
        if (!active || !isEnabled) return
        playDownSound(MinecraftClient.getInstance().soundManager)
        state = !state
        onToggle(state)
        animationStartTime = System.currentTimeMillis()
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }
}
