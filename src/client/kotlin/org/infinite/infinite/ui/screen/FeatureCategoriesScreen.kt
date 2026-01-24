package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.infinite.ui.widget.CategoryWidget
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.ui.screen.AbstractCarouselScreen
import org.infinite.utils.alpha
import org.lwjgl.glfw.GLFW
import kotlin.reflect.KClass

abstract class FeatureCategoriesScreen<K : KClass<out Feature>, V : Feature, T : Category<K, V>, W : CategoryWidget<T>>(
    parent: Screen? = null,
) :
    AbstractCarouselScreen<T>(Component.literal("Infinite Client"), parent) {

    abstract override val dataSource: List<T>

    private var searchBox: EditBox? = null
    private var lastQuery: String = ""
    private val searchHeight = 22
    private val searchTop = 16

    private fun updateSearch(query: String) {
        lastQuery = query
        for (widget in carouselWidgets) {
            if (widget is CategoryWidget<*>) {
                widget.setSearchQuery(query)
            }
        }
    }

    override fun init() {
        super.init()

        val boxWidth = (width * 0.55f).coerceIn(240f, 520f).toInt()
        val boxX = (width - boxWidth) / 2
        val search = EditBox(minecraft.font, boxX, searchTop, boxWidth, searchHeight, Component.literal("Search"))
        search.setMaxLength(60)
        search.value = lastQuery
        search.setResponder { updateSearch(it) }
        searchBox = search
        updateSearch(search.value)
    }

    override val lerpFactor: Float
        get() = 0.8f

    // 戻り値を AbstractCarouselWidget<T> ではなく W にする
    abstract override fun createWidget(index: Int, data: T): W

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        try {
            renderBlurredBackground(guiGraphics)
        } catch (ex: IllegalStateException) {
            if (ex.message?.contains("blur", ignoreCase = true) == true) {
                renderTransparentBackground(guiGraphics)
            } else {
                throw ex
            }
        }

        val colorScheme = InfiniteClient.theme.colorScheme
        val backdrop = Graphics2DRenderer(guiGraphics)
        backdrop.fillStyle = colorScheme.backgroundColor.alpha(140)
        backdrop.fillRect(0f, 0f, width.toFloat(), height.toFloat())
        backdrop.flush()

        super.render(guiGraphics, mouseX, mouseY, delta)

        val search = searchBox
        if (search != null) {
            val glow = Graphics2DRenderer(guiGraphics)
            val glowPadding = 6f
            val radius = (search.height * 0.7f).coerceAtLeast(10f)
            val glowColor = if (search.isFocused) {
                colorScheme.accentColor
            } else {
                colorScheme.secondaryColor
            }
            glow.fillStyle = glowColor.alpha(if (search.isFocused) 80 else 50)
            glow.fillRoundedRect(
                search.x - glowPadding,
                search.y - glowPadding,
                (search.width + glowPadding * 2),
                (search.height + glowPadding * 2),
                radius,
            )
            glow.flush()

            search.render(guiGraphics, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val search = searchBox
        if (search != null) {
            if (search.mouseClicked(mouseButtonEvent, bl)) {
                return true
            }
            search.setFocused(false)
        }
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        val search = searchBox
        if (search != null && search.mouseReleased(mouseButtonEvent)) {
            return true
        }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        val search = searchBox
        if (search != null && search.mouseDragged(mouseButtonEvent, d, e)) {
            return true
        }
        return super.mouseDragged(mouseButtonEvent, d, e)
    }

    override fun mouseMoved(d: Double, e: Double) {
        searchBox?.mouseMoved(d, e)
        super.mouseMoved(d, e)
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        val search = searchBox
        if (search != null && search.mouseScrolled(d, e, f, g)) {
            return true
        }
        return super.mouseScrolled(d, e, f, g)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        val search = searchBox
        if (search != null && search.isFocused) {
            if (keyEvent.key == GLFW.GLFW_KEY_ESCAPE) {
                search.setFocused(false)
                return true
            }
            if (search.keyPressed(keyEvent)) {
                return true
            }
        }
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        val search = searchBox
        if (search != null && search.charTyped(characterEvent)) {
            return true
        }
        return super.charTyped(characterEvent)
    }
}
