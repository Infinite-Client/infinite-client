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
) : ScrollableLayout(Minecraft.getInstance(), layout, i),
    Renderable,
    GuiEventListener,
    NarratableEntry {

    private var focusedChild: GuiEventListener? = null

    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        visitWidgets { widget ->
            widget.render(guiGraphics, i, j, f)
        }
    }

    private var focused = false
    override fun getRectangle(): ScreenRectangle = ScreenRectangle(x, y, width, height)

    override fun setFocused(bl: Boolean) {
        focused = bl
        if (!bl) {
            focusedChild?.setFocused(false)
            focusedChild = null
        }
    }

    override fun isFocused(): Boolean = focused

    fun getFocused(): GuiEventListener? = focusedChild

    fun setFocused(listener: GuiEventListener?) {
        if (focusedChild != listener) {
            focusedChild?.setFocused(false)
            focusedChild = listener
            listener?.setFocused(true)
        }
    }

    fun children(): List<GuiEventListener> {
        val list = mutableListOf<GuiEventListener>()
        visitWidgets { list.add(it) }
        return list
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        // 子要素への委譲
        var result = false
        visitWidgets { widget ->
            if (!result && widget.mouseScrolled(d, e, f, g)) {
                result = true
            }
        }
        if (result) return true

        // 自身のスクロール（スクロールバー等）
        return super.mouseScrolled(d, e, f, g)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        focusedChild?.mouseReleased(mouseButtonEvent)
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun mouseMoved(d: Double, e: Double) {
        super.mouseMoved(d, e)
        visitWidgets { it.mouseMoved(d, e) }
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        if (focusedChild?.mouseDragged(mouseButtonEvent, d, e) == true) return true
        return super.mouseDragged(mouseButtonEvent, d, e)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        var hitChild = false
        visitWidgets { child ->
            if (!hitChild && child.mouseClicked(mouseButtonEvent, bl)) {
                setFocused(child)
                hitChild = true
            }
        }

        if (hitChild) return true

        // 子要素にヒットしなかった場合、フォーカスをクリア（任意）
        if (super.mouseClicked(mouseButtonEvent, bl)) return true

        return false
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean = focusedChild?.charTyped(characterEvent) ?: false

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (focusedChild?.keyPressed(keyEvent) == true) return true
        return super.keyPressed(keyEvent)
    }

    override fun keyReleased(keyEvent: KeyEvent): Boolean {
        if (focusedChild?.keyReleased(keyEvent) == true) return true
        return super.keyReleased(keyEvent)
    }

    override fun narrationPriority(): NarratableEntry.NarrationPriority = NarratableEntry.NarrationPriority.FOCUSED

    override fun updateNarration(narrationElementOutput: NarrationElementOutput) {
    }
}
