package org.infinite.libs.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.infinite.features.Feature
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.utils.rendering.drawBorder

class InfiniteFeatureToggle(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val feature: Feature<out ConfigurableFeature>,
    private val isSelected: Boolean, // New parameter
    val onSettings: () -> Unit, // Made public
) : AbstractWidget(x, y, width, height, Component.literal(feature.name)) {
    val toggleButton: InfiniteToggleButton
    private val settingsButton: InfiniteButton
    private val resetButton: InfiniteButton // New reset button

    init {
        val buttonWidth = 50
        val settingsButtonWidth = 20
        val resetButtonWidth = 20 // Width for the reset button
        val spacing = 5

        val configurableFeature = feature.instance
        toggleButton =
            InfiniteToggleButton(
                x + width - buttonWidth,
                y,
                buttonWidth,
                height,
                configurableFeature.isEnabled(),
                configurableFeature.togglable,
            ) { newState ->
                if (newState) {
                    configurableFeature.enable()
                } else {
                    configurableFeature.disable()
                }
            }

        settingsButton =
            InfiniteButton(
                x + width - buttonWidth - spacing - settingsButtonWidth,
                y,
                settingsButtonWidth,
                height,
                Component.literal("S"),
            ) { onSettings() }

        resetButton =
            InfiniteButton(
                x + width - buttonWidth - spacing * 2 - settingsButtonWidth - resetButtonWidth,
                y,
                resetButtonWidth,
                height,
                Component.literal("R"), // Placeholder for reset icon/text
            ) {
                // OnPress action for reset button
                configurableFeature.reset() // Reset feature's enabled state
                configurableFeature.settings.forEach { setting ->
                    setting.reset() // Reset individual settings
                }
                InfiniteClient.log(Component.translatable("command.infinite.config.reset.feature", feature.name).string)
            }

        // Add listener to update toggle button when feature.enabled changes
        configurableFeature.addEnabledChangeListener { _, newValue ->
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

        // Draw button text
        graphics2D.drawText(
            Component.literal(feature.name),
            x + 60,
            y + (height - 8) / 2,
            InfiniteClient
                .currentColors()
                .foregroundColor,
            true, // shadow = true
        )

        toggleButton.x = x + width - toggleButton.width
        toggleButton.y = y
        settingsButton.x = x + width - toggleButton.width - 5 - settingsButton.width
        settingsButton.y = y
        resetButton.x =
            x + width - toggleButton.width - 5 * 2 - settingsButton.width - resetButton.width // Position reset button
        resetButton.y = y

        toggleButton.render(context, mouseX, mouseY, delta)
        settingsButton.render(context, mouseX, mouseY, delta)
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
    ): Boolean = toggleButton.mouseClicked(click, doubled) ||
        settingsButton.mouseClicked(click, doubled) ||
        resetButton.mouseClicked(click, doubled) // Handle reset button click

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        this.defaultButtonNarrationText(builder)
    }
}
