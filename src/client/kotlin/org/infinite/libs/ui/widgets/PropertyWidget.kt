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
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.ListProperty
import org.infinite.libs.core.features.property.NumberProperty
import org.infinite.libs.core.features.property.SelectionProperty
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
        val rawDescription = translationKey
            ?.let { Component.translatable(it).string }
            ?.takeIf { it != translationKey }
            ?: ""
        val description = buildDescription(rawDescription, property)
        val name = property.name
        val lineHeight = Minecraft.getInstance().font.lineHeight.toFloat()
        val nameSize = lineHeight
        val descriptionSize = lineHeight
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

    private fun buildDescription(raw: String, property: Property<*>): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.any { it == '.' || it == '!' || it == '?' }) return trimmed
        val suffix = if (trimmed.endsWith(".")) "" else "."
        return when (property) {
            is BooleanProperty -> {
                val verbPrefixes = listOf(
                    "Allow ",
                    "Show ",
                    "Keep ",
                    "Ignore ",
                    "Release ",
                    "Pause ",
                    "Target ",
                    "Use ",
                )
                val base = if (verbPrefixes.any { trimmed.startsWith(it) }) trimmed else "Toggle $trimmed"
                base + suffix
            }
            is SelectionProperty<*> -> "Select $trimmed$suffix"
            is NumberProperty<*> -> "Adjust $trimmed$suffix"
            is ListProperty<*> -> "Edit $trimmed$suffix"
            else -> "Set $trimmed$suffix"
        }
    }
}
