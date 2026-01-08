package org.infinite.libs.ui.layout

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.ScrollableLayout
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.Layout
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent

class IScrollableLayout(
    minecraft: Minecraft,
    layout: Layout,
    i: Int,
) : ScrollableLayout(minecraft, layout, i), Renderable, GuiEventListener {

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
        // 自身のスクロール処理を優先（superはスクロールバーの制御を行う）
        if (super.mouseScrolled(d, e, f, g)) return true

        var result = false
        visitWidgets { widget ->
            if (!result && widget.mouseScrolled(d, e, f, g)) {
                result = true
            }
        }
        return result
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        if (super.mouseReleased(mouseButtonEvent)) return true

        var result = false
        visitWidgets { widget ->
            if (!result && widget.mouseReleased(mouseButtonEvent)) {
                result = true
            }
        }
        return result
    }

    override fun mouseMoved(d: Double, e: Double) {
        super.mouseMoved(d, e)
        visitWidgets { widget ->
            widget.mouseMoved(d, e)
        }
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        if (super.mouseDragged(mouseButtonEvent, d, e)) return true

        var result = false
        visitWidgets { widget ->
            if (!result && widget.mouseDragged(mouseButtonEvent, d, e)) {
                result = true
            }
        }
        return result
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (super.mouseClicked(mouseButtonEvent, bl)) return true

        var result = false
        visitWidgets { widget ->
            // クリック範囲内にある子要素にのみ伝播
            if (!result && widget.isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
                if (widget.mouseClicked(mouseButtonEvent, bl)) {
                    result = true
                }
            }
        }
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
        if (super.keyPressed(keyEvent)) return true

        var result = false
        visitWidgets { widget ->
            if (!result && widget.keyPressed(keyEvent)) {
                result = true
            }
        }
        return result
    }

    override fun keyReleased(keyEvent: KeyEvent): Boolean {
        var result = false
        visitWidgets { widget ->
            if (!result && widget.keyReleased(keyEvent)) {
                result = true
            }
        }
        return result
    }

    override fun isFocused(): Boolean = focused
}
