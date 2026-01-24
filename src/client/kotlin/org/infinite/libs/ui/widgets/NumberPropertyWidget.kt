package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.property.NumberProperty
import org.infinite.libs.graphics.bundle.Graphics2DRenderer

class NumberPropertyWidget<T>(
    x: Int,
    y: Int,
    width: Int,
    property: NumberProperty<T>,
) : PropertyWidget<NumberProperty<T>>(
    x,
    y,
    width,
    DEFAULT_WIDGET_HEIGHT * 2,
    property,
) where T : Number, T : Comparable<T> {

    private class PropertySliderWidget<T>(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        private val property: NumberProperty<T>,
    ) : SliderWidget<T>(x, y, width, height) where T : Number, T : Comparable<T> {

        override val minValue: T get() = property.min
        override val maxValue: T get() = property.max
        override var value: T
            get() = property.value
            set(v) {
                property.value = v
            }

        @Suppress("UNCHECKED_CAST")
        override fun convertToType(v: Double): T = when (property.value) {
            is Int -> v.toInt()
            is Float -> v.toFloat()
            is Long -> v.toLong()
            is Byte -> v.toInt().toByte()
            is Short -> v.toInt().toShort()
            is Double -> v
            else -> v as Any // 基本的にはここに来ない
        } as T
    }

    // スライダー本体。初期位置は relocate() で制御
    private val propertySliderWidget = PropertySliderWidget(x, y + height, width, height, property)

    override fun children(): List<GuiEventListener> = listOf(propertySliderWidget)

    override fun relocate() {
        super.relocate()
        propertySliderWidget.width = width
        propertySliderWidget.height = DEFAULT_WIDGET_HEIGHT
        propertySliderWidget.x = x
        propertySliderWidget.y = y + height - propertySliderWidget.height
    }

    override fun renderWidget(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        super.renderWidget(guiGraphics, i, j, f)
        propertySliderWidget.render(guiGraphics, i, j, f)
        val colorScheme = InfiniteClient.theme.colorScheme
        val displayText = property.display()
        val g2d = Graphics2DRenderer(guiGraphics)
        g2d.fillStyle = colorScheme.foregroundColor
        g2d.textStyle.font = "infinite_regular"
        g2d.textStyle.size = Minecraft.getInstance().font.lineHeight.toFloat()
        g2d.textStyle.shadow = true
        g2d.textRight(displayText, x + width, y)
        g2d.flush()
    }
}
