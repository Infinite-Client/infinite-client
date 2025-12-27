package org.infinite.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.libs.graphics.Graphics2D
import org.infinite.settings.FeatureSetting

class InfiniteSettingToggle(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val setting: FeatureSetting.BooleanSetting,
) : AbstractWidget(x, y, width, height, Component.literal(setting.name)) {
    private val toggleButton: InfiniteToggleButton
    private val textRenderer = Minecraft.getInstance().font

    init {
        val buttonWidth = 50
        toggleButton =
            InfiniteToggleButton(
                x + width - buttonWidth,
                y,
                buttonWidth,
                height,
                setting.value,
                true, // isEnabled
            ) { newState ->
                setting.value = newState
            }

        // Add change listener to update the toggle button's state when the setting value changes externally
        setting.addChangeListener {
            toggleButton.setState(it)
        }
    }

    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)

        val textX = x + 5 // Padding from left edge
        val totalTextHeight: Int
        val nameY: Int
        val descriptionY: Int?

        if (setting.descriptionKey.isNotBlank()) {
            totalTextHeight = textRenderer.lineHeight * 2 + 2 // Name + padding + Description
            nameY = y + (height - totalTextHeight) / 2
            descriptionY = nameY + textRenderer.lineHeight + 2

            graphics2D.drawText(
                Component.translatable(setting.name),
                textX,
                nameY,
                org.infinite.InfiniteClient
                    .currentColors()
                    .foregroundColor,
                true, // shadow = true
            )
            graphics2D.drawText(
                Component.translatable(setting.descriptionKey),
                textX,
                descriptionY,
                org.infinite.InfiniteClient
                    .currentColors()
                    .foregroundColor,
                true, // shadow = true
            )
        } else {
            totalTextHeight = textRenderer.lineHeight // Only name
            nameY = y + (height - totalTextHeight) / 2

            graphics2D.drawText(
                Component.translatable(setting.name),
                textX,
                nameY,
                org.infinite.InfiniteClient
                    .currentColors()
                    .foregroundColor,
                true, // shadow = true
            )
        }

        toggleButton.x = x + width - toggleButton.width
        toggleButton.y = y
        toggleButton.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean = toggleButton.mouseClicked(click, doubled)

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        this.defaultButtonNarrationText(builder)
    }
}
