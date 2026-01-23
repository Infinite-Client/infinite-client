package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.infinite.ui.ClickGuiPalette
import org.infinite.infinite.ui.screen.FeatureScreen
import org.infinite.libs.core.features.Property
import org.infinite.libs.graphics.bundle.Graphics2DRenderer

open class PropertyWidget<T : Property<*>>(
    x: Int,
    y: Int,
    width: Int,
    height: Int = DEFAULT_WIDGET_HEIGHT,
    protected val property: T,
) :
    AbstractContainerWidget(x, y, width, height, Component.literal("")), Renderable {
    companion object {
        protected const val DEFAULT_WIDGET_HEIGHT = 20
    }

    override fun contentHeight(): Int = this.height

    override fun scrollRate(): Double = 10.0

    override fun children(): List<GuiEventListener> =
        listOf()

    override fun setWidth(i: Int) {
        super.setWidth(i)
        relocate()
    }

    override fun setHeight(i: Int) {
        super.setHeight(i)
        relocate()
    }

    override fun setX(i: Int) {
        super.setX(i)
        relocate()
    }

    override fun setY(i: Int) {
        super.setY(i)
        relocate()
    }

    override fun setSize(i: Int, j: Int) {
        super.setSize(i, j)
        relocate()
    }

    override fun setPosition(i: Int, j: Int) {
        super.setPosition(i, j)
        relocate()
    }

    override fun setRectangle(i: Int, j: Int, k: Int, l: Int) {
        super.setRectangle(i, j, k, l)
        relocate()
    }

    protected open fun relocate() {}

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        i: Int,
        j: Int,
        f: Float,
    ) {
        val g2d = Graphics2DRenderer(guiGraphics)
        val colorScheme = InfiniteClient.theme.colorScheme
        val usePalette = Minecraft.getInstance().screen is FeatureScreen<*>
        val translationKey = property.translationKey()
        val translated = translationKey?.let { Component.translatable(it).string }
        val name = if (translated != null && translated != translationKey) translated else property.name
        val description = ""
        val nameSize = 10f
        val descriptionSize = 8f
        val padding = 2f
        g2d.textStyle.size = nameSize
        g2d.textStyle.shadow = false
        g2d.textStyle.font = "infinite_regular"
        g2d.fillStyle = if (usePalette) ClickGuiPalette.TEXT else colorScheme.foregroundColor
        g2d.text(name, x, y)
        if (description.isNotBlank()) {
            g2d.textStyle.size = descriptionSize
            g2d.fillStyle = if (usePalette) ClickGuiPalette.MUTED else colorScheme.secondaryColor
            g2d.text(description, x.toFloat(), y + nameSize + padding)
        }
        g2d.flush()
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }
}
