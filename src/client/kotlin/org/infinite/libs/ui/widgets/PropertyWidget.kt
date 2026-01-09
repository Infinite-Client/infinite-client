package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Property
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.log.LogSystem

open class PropertyWidget<T : Property<*>>(
    x: Int,
    y: Int,
    width: Int,
    height: Int = DEFAULT_WIDGET_HEIGHT,
    private val property: T,
) :
    AbstractContainerWidget(x, y, width, height, Component.literal("")), Renderable {
    companion object {
        private const val DEFAULT_WIDGET_HEIGHT = 20
    }

    override fun contentHeight(): Int = this.height

    override fun scrollRate(): Double = 10.0

    override fun children(): List<GuiEventListener> =
        listOf()

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        return super.mouseScrolled(d, e, f, g)
    }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        i: Int,
        j: Int,
        f: Float,
    ) {
        val g2d = Graphics2DRenderer(guiGraphics)
        val colorScheme = InfiniteClient.theme.colorScheme
        val name = property.name
        val translationKey = property.translationKey() ?: "unknown"
        val description = Component.translatable(translationKey).string
        g2d.textStyle.size = 10f
        g2d.textStyle.shadow = false
        g2d.textStyle.font = "infinite_regular"
        g2d.fillStyle = colorScheme.foregroundColor
        g2d.text(name, x, y)
        g2d.textStyle.size = 8f
        g2d.fillStyle = colorScheme.secondaryColor
        LogSystem.log("RENDER")
        g2d.text(description, x.toFloat(), y + height - g2d.textStyle.size)
        g2d.flush()
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }
}
