package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature.HudElement
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class UltraHudEditorScreen(
    private val parent: Screen?,
) : Screen(Component.literal("Ultra HUD Editor")) {
    private val ultraUiFeature: UltraUiFeature
        get() = InfiniteClient.localFeatures.rendering.ultraUiFeature

    private var selectedElement: HudElement? = null
    private var draggingElement: HudElement? = null
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    override fun extractBackground(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractBackground(guiGraphics, mouseX, mouseY, delta)
        val g2d = Graphics2DRenderer(guiGraphics)
        InfiniteClient.theme.renderBackGround(0f, 0f, width.toFloat(), height.toFloat(), g2d, 0.18f)
        g2d.flush()
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val g2d = Graphics2DRenderer(guiGraphics)
        val scheme = InfiniteClient.theme.colorScheme
        val rects = ultraUiFeature.elementRects(g2d)
        val hovered = rects.entries.lastOrNull { it.value.contains(mouseX.toDouble(), mouseY.toDouble()) }?.key

        g2d.fillStyle = scheme.backgroundColor.alpha(140)
        g2d.fillRoundedRect(16f, 16f, 196f, 72f, 10f)
        g2d.strokeStyle.width = 1f
        g2d.strokeStyle.color = scheme.accentColor.alpha(180)
        g2d.strokeRoundedRect(16f, 16f, 196f, 72f, 10f)
        g2d.textStyle.font = "infinite_bolditalic"
        g2d.textStyle.shadow = true
        g2d.textStyle.size = 10f
        g2d.fillStyle = scheme.foregroundColor
        g2d.text("HUD Editor", 28f, 28f)
        g2d.textStyle.font = "infinite_regular"
        g2d.textStyle.size = 8f
        g2d.fillStyle = scheme.secondaryColor
        g2d.text("Drag boxes. Right click or R resets.", 28f, 44f)
        g2d.text("Shift+R resets all. ESC closes.", 28f, 58f)

        rects.forEach { (element, rect) ->
            val selected = element == selectedElement
            val isHovered = element == hovered
            val frameColor = when {
                selected -> scheme.accentColor.alpha(230)
                isHovered -> scheme.secondaryColor.alpha(200)
                else -> scheme.foregroundColor.alpha(120)
            }
            val fillColor = when (element) {
                HudElement.Crosshair -> scheme.accentColor.alpha(if (selected) 70 else 36)
                HudElement.Hotbar -> scheme.color(35f, 0.6f, 0.8f).alpha(if (selected) 88 else 48)
                HudElement.TopBox -> scheme.color(120f, 0.55f, 0.8f).alpha(if (selected) 88 else 48)
                HudElement.LeftBox -> scheme.color(0f, 0.6f, 0.8f).alpha(if (selected) 88 else 48)
                HudElement.RightBox -> scheme.color(210f, 0.6f, 0.8f).alpha(if (selected) 88 else 48)
            }

            g2d.fillStyle = fillColor
            g2d.fillRoundedRect(rect.x, rect.y, rect.width, rect.height, 8f)
            g2d.strokeStyle.color = frameColor
            g2d.strokeRoundedRect(rect.x, rect.y, rect.width, rect.height, 8f)

            if (selected) {
                g2d.strokeStyle.color = scheme.accentColor.alpha(90)
                g2d.strokeRoundedRect(rect.x - 2f, rect.y - 2f, rect.width + 4f, rect.height + 4f, 10f)
            }

            g2d.fillStyle = scheme.foregroundColor
            g2d.textStyle.size = 8f
            g2d.textCentered(element.label(), rect.x + rect.width / 2f, rect.y + rect.height / 2f - 4f)
        }

        selectedElement?.let { element ->
            g2d.fillStyle = scheme.backgroundColor.alpha(155)
            g2d.fillRoundedRect(16f, height - 42f, 160f, 22f, 8f)
            g2d.strokeStyle.color = scheme.accentColor.alpha(160)
            g2d.strokeRoundedRect(16f, height - 42f, 160f, 22f, 8f)
            g2d.fillStyle = scheme.foregroundColor
            g2d.text("${element.label()} ${ultraUiFeature.offsetText(element)}", 26f, height - 35f)
        }

        g2d.flush()
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val rects = ultraUiFeature.elementRects(Graphics2D())
        val clicked = rects.entries.lastOrNull { it.value.contains(mouseButtonEvent.x, mouseButtonEvent.y) }?.key
        selectedElement = clicked
        if (clicked != null && mouseButtonEvent.button() == 0) {
            draggingElement = clicked
            lastMouseX = mouseButtonEvent.x
            lastMouseY = mouseButtonEvent.y
            return true
        }
        if (clicked != null && mouseButtonEvent.button() == 1) {
            resetElement(clicked)
            return true
        }
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        val dragging = draggingElement ?: return super.mouseDragged(mouseButtonEvent, d, e)
        val dx = (mouseButtonEvent.x - lastMouseX).roundToInt()
        val dy = (mouseButtonEvent.y - lastMouseY).roundToInt()
        if (dx != 0 || dy != 0) {
            ultraUiFeature.applyDrag(dragging, dx, dy)
            lastMouseX = mouseButtonEvent.x
            lastMouseY = mouseButtonEvent.y
        }
        return true
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        draggingElement = null
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        when (keyEvent.key) {
            GLFW.GLFW_KEY_ESCAPE -> {
                onClose()
                return true
            }

            GLFW.GLFW_KEY_R -> {
                if (keyEvent.modifiers and GLFW.GLFW_MOD_SHIFT != 0) {
                    ultraUiFeature.resetOffsets()
                } else {
                    selectedElement?.let { resetElement(it) }
                }
                return true
            }
        }
        return super.keyPressed(keyEvent)
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen(): Boolean = false

    private fun resetElement(element: HudElement) {
        when (element) {
            HudElement.Hotbar -> {
                ultraUiFeature.hotbarOffsetX.reset()
                ultraUiFeature.hotbarOffsetY.reset()
            }

            HudElement.TopBox -> {
                ultraUiFeature.topBoxOffsetX.reset()
                ultraUiFeature.topBoxOffsetY.reset()
            }

            HudElement.LeftBox -> {
                ultraUiFeature.leftBoxOffsetX.reset()
                ultraUiFeature.leftBoxOffsetY.reset()
            }

            HudElement.RightBox -> {
                ultraUiFeature.rightBoxOffsetX.reset()
                ultraUiFeature.rightBoxOffsetY.reset()
            }

            HudElement.Crosshair -> {
                ultraUiFeature.crosshairOffsetX.reset()
                ultraUiFeature.crosshairOffsetY.reset()
            }
        }
    }

    private fun HudElement.label(): String = when (this) {
        HudElement.Hotbar -> "Hotbar"
        HudElement.TopBox -> "Top Box"
        HudElement.LeftBox -> "Left Box"
        HudElement.RightBox -> "Right Box"
        HudElement.Crosshair -> "Crosshair"
    }
}
