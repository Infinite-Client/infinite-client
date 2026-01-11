package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.ui.layout.ScrollableLayoutContainer
import org.lwjgl.glfw.GLFW

class FeatureScreen<T : Feature>(
    private val feature: T,
    private val parent: Screen,
) : Screen(Component.translatable(feature.translation())) {

    private lateinit var container: ScrollableLayoutContainer

    // レイアウト定数
    private val headerHeight = 60
    private val margin = 10

    override fun init() {
        feature.ensureAllPropertiesRegistered()
        val innerWidth = width - (margin * 2)

        // 内部レイアウトの構築
        val innerLayout = LinearLayout.vertical().spacing(margin)

        feature.properties.forEach { (_, property) ->
            val propertyWidget = property.widget(0, 0, innerWidth - margin * 2)
            innerLayout.addChild(propertyWidget)
        }
        innerLayout.arrangeElements()
        container = ScrollableLayoutContainer(minecraft, innerLayout, innerWidth).apply {
            this.x = margin
            this.y = headerHeight
            this.setMinWidth(innerWidth)
            this.setMaxHeight(parent.height - headerHeight - margin)
        }
        this.addRenderableWidget(container)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(guiGraphics, mouseX, mouseY, delta)
        val g2d = Graphics2DRenderer(guiGraphics)
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val centerX = width / 2f
        val size = 24f
        theme.renderBackGround(0, 0, this.width, this.height, g2d, 0.5f)
        g2d.fillStyle = when (feature.featureType) {
            Feature.FeatureType.Cheat -> colorScheme.redColor
            Feature.FeatureType.Extend -> colorScheme.yellowColor
            Feature.FeatureType.Utils -> colorScheme.greenColor
        }
        g2d.textStyle.size = size
        g2d.textStyle.font = "infinite_bolditalic"
        g2d.textStyle.shadow = true
        g2d.textCentered(feature.name, centerX, size)
        g2d.textStyle.size = size / 2f
        g2d.textStyle.font = "infinite_italic"
        g2d.textStyle.shadow = false
        g2d.fillStyle = colorScheme.secondaryColor
        val description = Component.translatable(feature.translation()).string
        g2d.textCentered(description, centerX, 2f * size)
        g2d.flush()
        container.render(guiGraphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        return container.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        return container.mouseDragged(mouseButtonEvent, d, e)
    }

    override fun mouseMoved(d: Double, e: Double) {
        container.mouseMoved(d, e)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        return container.mouseReleased(mouseButtonEvent)
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        return container.mouseScrolled(d, e, f, g)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(parent)
            return true
        }
        return container.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        return container.charTyped(characterEvent)
    }

    override fun children(): List<GuiEventListener> = listOf(container)

    override fun onClose() {
        minecraft.setScreen(parent)
    }
}
