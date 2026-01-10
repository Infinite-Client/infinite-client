package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import org.infinite.libs.core.features.property.BooleanProperty

class BooleanPropertyWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int = DEFAULT_WIDGET_HEIGHT,
    property: BooleanProperty,
) : PropertyWidget<BooleanProperty>(x, y, width, height, property) {
    private class PropertyToggleButton(x: Int, y: Int, width: Int, height: Int, private val property: BooleanProperty) :
        ToggleButton(x, y, width, height) {
        override var value: Boolean
            get() = property.value
            set(value) {
                if (property.value != value) property.toggle()
            }
    }

    private val propertyToggleButton = PropertyToggleButton(x, y, height * 2, height, property)
    override fun children(): List<GuiEventListener> = listOf(propertyToggleButton)
    override fun relocate() {
        super.relocate()
        propertyToggleButton.x = x + width - propertyToggleButton.width
        propertyToggleButton.y = y
        propertyToggleButton.width = height * 2
        propertyToggleButton.height = height
    }

    override fun renderWidget(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        super.renderWidget(guiGraphics, i, j, f)
        propertyToggleButton.render(guiGraphics, i, j, f)
    }
}
