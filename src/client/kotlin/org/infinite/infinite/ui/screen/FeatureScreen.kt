package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.ClickGuiPalette
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.ui.layout.ScrollableLayoutContainer
import org.lwjgl.glfw.GLFW

class FeatureScreen<T : Feature>(
    private val feature: T,
    private val parent: Screen,
) : Screen(Component.literal(feature.name)) {

    private lateinit var container: ScrollableLayoutContainer

    private val panelRadius = 12f
    private val headerHeight = 44
    private val panelPadding = 16
    private val screenPadding = 24
    private val scrollbarWidth = 20

    private var panelX = 0
    private var panelY = 0
    private var panelW = 0
    private var panelH = 0

    override fun init() {
        clearWidgets()
        feature.ensureAllPropertiesRegistered()

        val maxPanelW = (width - screenPadding * 2).coerceAtLeast(200)
        val maxPanelH = (height - screenPadding * 2).coerceAtLeast(180)
        val minPanelW = minOf(320, maxPanelW)
        val minPanelH = minOf(220, maxPanelH)
        panelW = (width * 0.72f).toInt().coerceIn(minPanelW, maxPanelW)
        panelH = (height * 0.78f).toInt().coerceIn(minPanelH, maxPanelH)
        panelX = (width - panelW) / 2
        panelY = (height - panelH) / 2

        val availableWidth = panelW - panelPadding * 2
        val innerWidth = (availableWidth - scrollbarWidth).coerceAtLeast(120)
        val innerLayout = LinearLayout.vertical().spacing(8)

        feature.properties.forEach { (_, property) ->
            val propertyWidget = property.widget(0, 0, innerWidth)
            innerLayout.addChild(propertyWidget)
        }
        innerLayout.arrangeElements()
        container = ScrollableLayoutContainer(innerLayout, innerWidth).apply {
            x = panelX + panelPadding
            y = panelY + headerHeight
            setMinWidth(availableWidth)
            setMaxHeight(panelH - headerHeight - panelPadding)
        }
        addRenderableWidget(container)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // Custom background render in render().
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val g2d = Graphics2DRenderer(guiGraphics)
        g2d.fillStyle = ClickGuiPalette.BACKDROP
        g2d.fillRoundedRect(0f, 0f, width.toFloat(), height.toFloat(), 0f)
        g2d.fillStyle = ClickGuiPalette.PANEL
        g2d.fillRoundedRect(panelX.toFloat(), panelY.toFloat(), panelW.toFloat(), panelH.toFloat(), panelRadius)
        g2d.fillStyle = ClickGuiPalette.PANEL_ALT
        val headerRadius = panelRadius.coerceAtMost(headerHeight / 2f)
        g2d.fillRoundedRect(panelX.toFloat(), panelY.toFloat(), panelW.toFloat(), headerHeight.toFloat(), headerRadius)
        if (headerHeight > headerRadius) {
            g2d.fillRect(
                panelX.toFloat(),
                panelY.toFloat() + headerHeight - headerRadius,
                panelW.toFloat(),
                headerRadius,
            )
        }
        g2d.flush()

        val font = minecraft.font
        val titleX = panelX + panelPadding
        val titleY = panelY + 8
        val title = feature.name
        guiGraphics.drawString(font, title, titleX, titleY, ClickGuiPalette.TEXT, false)

        val descriptionKey = feature.translation()
        val description = Component.translatable(descriptionKey).string
        if (description != descriptionKey && description != feature.name) {
            val descY = titleY + font.lineHeight + 2
            guiGraphics.drawString(font, description, titleX, descY, ClickGuiPalette.MUTED, false)
        }

        super.render(guiGraphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean = container.mouseClicked(mouseButtonEvent, bl)

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean = container.mouseDragged(mouseButtonEvent, d, e)

    override fun mouseMoved(d: Double, e: Double) {
        container.mouseMoved(d, e)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean = container.mouseReleased(mouseButtonEvent)

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean = container.mouseScrolled(d, e, f, g)

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(parent)
            return true
        }
        return container.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean = container.charTyped(characterEvent)

    override fun children(): List<GuiEventListener> = listOf(container)

    override fun onClose() {
        minecraft.setScreen(parent)
    }
}
