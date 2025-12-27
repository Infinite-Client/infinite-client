package org.infinite.libs.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.global.ConfigurableGlobalFeature
import org.infinite.global.GlobalFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.utils.rendering.drawBorder

class InfiniteGlobalFeatureToggle(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val globalFeature: GlobalFeature<out ConfigurableGlobalFeature>,
    private val isSelected: Boolean, // New parameter
    private val featureDescription: String,
) : AbstractWidget(x, y, width, height, Component.literal(globalFeature.name)) {
    val toggleButton: InfiniteToggleButton
    private val resetButton: InfiniteButton // New reset button
    private val textRenderer = Minecraft.getInstance().font

    init {
        val buttonWidth = 50
        val resetButtonWidth = 20 // Width for the reset button
        val spacing = 5
        val configurableGlobalFeature = globalFeature.instance
        toggleButton =
            InfiniteToggleButton(
                x + width - buttonWidth,
                y,
                buttonWidth,
                height,
                configurableGlobalFeature.isEnabled(),
                configurableGlobalFeature.togglable,
            ) { newState ->
                if (newState) {
                    configurableGlobalFeature.enable()
                } else {
                    configurableGlobalFeature.disable()
                }
            }
        resetButton =
            InfiniteButton(
                x + width - buttonWidth - spacing * 2 - resetButtonWidth,
                y,
                resetButtonWidth,
                height,
                Component.literal("R"), // Placeholder for reset icon/text
            ) {
                // OnPress action for reset button
                configurableGlobalFeature.reset() // Reset feature's enabled state
                configurableGlobalFeature.settings.forEach { setting ->
                    setting.reset() // Reset individual settings
                }
                InfiniteClient.log(
                    Component
                        .translatable(
                            "command.infinite.config.reset.globalFeature",
                            globalFeature.name,
                        ).string,
                )
            }

        // Add listener to update toggle button when feature.enabled changes
        configurableGlobalFeature.addEnabledChangeListener { _, newValue ->
            toggleButton.setState(newValue)
        }
    }

    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)

        graphics2D.drawText(
            Component.literal(globalFeature.name),
            x + 5, // 左端から少しパディング
            y + (height - textRenderer.lineHeight) / 2, // 垂直方向中央
            InfiniteClient.currentColors().foregroundColor,
            true, // shadow = true
        )

        var descriptionY = y + (height - textRenderer.lineHeight) / 2 + textRenderer.lineHeight + 2 // タイトルの下2ピクセル

        featureDescription.split("\n").forEach { line ->
            graphics2D.drawText(
                Component.literal(line),
                x + 5, // 左端から少しパディング
                descriptionY,
                InfiniteClient.currentColors().secondaryColor,
                true, // shadow = true
            )
            descriptionY += textRenderer.lineHeight + 1 // 次の行へ
        }
        toggleButton.x = x + width - toggleButton.width
        toggleButton.y = y
        resetButton.x =
            x + width - toggleButton.width - 5 - resetButton.width // Position reset button
        resetButton.y = y
        toggleButton.render(context, mouseX, mouseY, delta)
        resetButton.render(context, mouseX, mouseY, delta) // Render reset button
        if (isSelected) {
            val interpolatedColor =
                InfiniteClient
                    .currentColors()
                    .primaryColor
            context.drawBorder(x, y, width, height, interpolatedColor)
        }
    }

    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean =
        toggleButton.mouseClicked(click, doubled) ||
            resetButton.mouseClicked(click, doubled) // Handle reset button click

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        this.defaultButtonNarrationText(builder)
    }
}
