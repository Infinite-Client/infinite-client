package org.infinite.libs.ui.layout

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.ScrollableLayout
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.Layout
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent

class ScrollableLayoutContainer(
    layout: Layout,
    i: Int,
) : ScrollableLayout(Minecraft.getInstance(), layout, i), Renderable, GuiEventListener, NarratableEntry {
    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        visitWidgets { widget ->
            widget.render(guiGraphics, i, j, f)
        }
    }

    private var focused = false
    override fun getRectangle(): ScreenRectangle {
        return ScreenRectangle(x, y, width, height)
    }

    override fun setFocused(bl: Boolean) {
        focused = bl
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        var result = false
        visitWidgets { widget ->
            if (!result && widget.mouseScrolled(d, e, f, g)) {
                result = true
            }
        }
        if (super.mouseScrolled(d, e, f, g)) return true
        return result
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        var result = false
        visitWidgets { widget ->
            if (!result && widget.mouseReleased(mouseButtonEvent)) {
                result = true
            }
        }
        if (super.mouseReleased(mouseButtonEvent)) return true
        return result
    }

    override fun mouseMoved(d: Double, e: Double) {
        super.mouseMoved(d, e)
        visitWidgets { widget ->
            widget.mouseMoved(d, e)
        }
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        var result = false
        visitWidgets { widget ->
            if (!result && widget.mouseDragged(mouseButtonEvent, d, e)) {
                result = true
            }
        }
        if (super.mouseDragged(mouseButtonEvent, d, e)) return true
        return result
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        var result = false
        visitWidgets { widget ->
            if (!result && widget.mouseClicked(mouseButtonEvent, bl)) {
                result = true
            }
        }
        if (super.mouseClicked(mouseButtonEvent, bl)) return true
        return result
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        var result = false
        visitWidgets { widget ->
            if (!result && widget.charTyped(characterEvent)) {
                result = true
            }
        }
        return result
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        var result = false
        visitWidgets { widget ->
            if (!result && widget.keyPressed(keyEvent)) {
                result = true
            }
        }
        if (super.keyPressed(keyEvent)) return true
        return result
    }

    override fun keyReleased(keyEvent: KeyEvent): Boolean {
        var result = false
        visitWidgets { widget ->
            if (!result && widget.keyReleased(keyEvent)) {
                result = true
            }
        }
        if (super.keyReleased(keyEvent)) return true
        return result
    }

    override fun isFocused(): Boolean = focused
    override fun narrationPriority(): NarratableEntry.NarrationPriority = NarratableEntry.NarrationPriority.FOCUSED

    override fun updateNarration(narrationElementOutput: NarrationElementOutput) {
    }
}
