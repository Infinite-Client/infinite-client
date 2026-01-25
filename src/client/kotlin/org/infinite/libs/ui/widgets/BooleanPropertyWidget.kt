package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import org.infinite.libs.core.features.property.BooleanProperty

class BooleanPropertyWidget(
    x: Int,
    y: Int,
    width: Int,
    property: BooleanProperty,
) : PropertyWidget<BooleanProperty>(
    x,
    y,
    width,
    DEFAULT_WIDGET_HEIGHT * 2,
    property,
) {
    private class PropertyToggleButton(x: Int, y: Int, width: Int, height: Int, private val property: BooleanProperty) : ToggleButton(x, y, width, height) {
        override var value: Boolean
            get() = property.value
            set(value) {
                if (property.value != value) property.toggle()
            }
    }

    private val propertyToggleButton = PropertyToggleButton(x, y, height * 2, DEFAULT_WIDGET_HEIGHT, property)
    override fun children(): List<GuiEventListener> = listOf(propertyToggleButton)
    override fun relocate() {
        super.relocate()
        val twoLineLimit = 256
        propertyToggleButton.width = propertyToggleButton.height * 2
        propertyToggleButton.x = x + width - propertyToggleButton.height * 2
        propertyToggleButton.y =
            if (width > twoLineLimit) {
                y
            } else {
                y + height - propertyToggleButton.height
            }
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderWidget(guiGraphics, mouseX, mouseY, delta)
        propertyToggleButton.render(guiGraphics, mouseX, mouseY, delta)
    }
}
