package org.infinite.libs.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.settings.FeatureSetting

class InfiniteSelectionListField(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val setting: FeatureSetting<*>,
) : AbstractWidget(x, y, width, height, Component.literal(setting.name)) {
    private val textRenderer = Minecraft.getInstance().font
    private var cycleButton: InfiniteButton
    private val buttonWidth: Int

    init {
        val options: List<String> =
            when (setting) {
                is FeatureSetting.EnumSetting<*> -> setting.options.map { it.name }
                is FeatureSetting.StringListSetting -> setting.options
                else -> emptyList()
            }
        buttonWidth =
            (
                options.maxOfOrNull { textRenderer.width(it) + 8 }
                    ?: 0
            ).coerceAtLeast(50)

        // Initialize cycleButton here
        cycleButton =
            InfiniteButton(
                x + width - buttonWidth, // Right-aligned
                y, // Vertically centered within the widget's height
                buttonWidth,
                height,
                getCurrentSettingValueAsText(),
            ) {
                cycleOption()
            }

        // Add change listener to update the button's message when the setting value changes externally
        setting.addChangeListener {
            cycleButton.message = getCurrentSettingValueAsText()
        }
    }

    private fun getCurrentSettingValueAsText(): Component =
        when (setting) {
            is FeatureSetting.EnumSetting<*> -> Component.literal(setting.value.name)
            is FeatureSetting.StringListSetting -> Component.literal(setting.value)
            else -> Component.literal("N/A")
        }

    private fun cycleOption() {
        when (setting) {
            // FeatureSetting.EnumSetting<*> の代わりに、ローカル変数として val enumSetting を定義し、
            // 以下のブロックで安全にキャストできることをコンパイラに伝えます。
            is FeatureSetting.EnumSetting<*> -> {
                val currentIndex = setting.options.indexOf(setting.value)
                val nextIndex = (currentIndex + 1) % setting.options.size
                setting.updateValueFromEnumStar(setting.options[nextIndex])
                cycleButton.message = Component.literal(setting.value.name)
            }

            is FeatureSetting.StringListSetting -> {
                val currentIndex = setting.options.indexOf(setting.value)
                val nextIndex = (currentIndex + 1) % setting.options.size
                setting.set(setting.options[nextIndex])
                cycleButton.message = Component.literal(setting.value)
            }

            else -> {}
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
                InfiniteClient
                    .currentColors()
                    .foregroundColor,
                true, // shadow = true
            )
            graphics2D.drawText(
                Component.translatable(setting.descriptionKey),
                textX,
                descriptionY,
                InfiniteClient
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
                InfiniteClient
                    .currentColors()
                    .foregroundColor,
                true, // shadow = true
            )
        }

        cycleButton.x = x + width - buttonWidth
        cycleButton.y = y
        cycleButton.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean = cycleButton.mouseClicked(click, doubled)

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        this.defaultButtonNarrationText(builder)
    }
}
