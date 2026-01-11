package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.property.ListProperty
import org.infinite.utils.mix

class ListPropertyWidget<T : Any>(
    x: Int,
    y: Int,
    width: Int,
    property: ListProperty<T>,
) : PropertyWidget<ListProperty<T>>(
    x,
    y,
    width,
    DEFAULT_WIDGET_HEIGHT * 6, // リスト表示用に最初から高さを固定で確保
    property,
) {
    private var activeInputWidget: AbstractWidget? = null
    private var editingIndex: Int = -1

    override fun relocate() {
        super.relocate()
        // activeInputWidgetの位置更新などは、render内、または固定高さ前提のy座標で行う
        activeInputWidget?.let {
            it.x = this.x + 5
            it.width = this.width - 10
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (activeInputWidget != null) {
            if (activeInputWidget!!.mouseClicked(mouseButtonEvent, bl)) {
                return true
            } else {
                // ウィジェット外をクリックしたら編集を終了（キャンセル）
                activeInputWidget = null
                editingIndex = -1
                return true // クリックを消費
            }
        }

        val mouseX = mouseButtonEvent.x
        val mouseY = mouseButtonEvent.y
        val itemHeight = 20
        var currentY = y + DEFAULT_WIDGET_HEIGHT

        // 各要素のループ
        property.value.forEachIndexed { index, item ->
            // 削除ボタン (右端)
            if (mouseX >= x + width - 20 && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + itemHeight) {
                property.removeAt(index)
                return true
            }
            // 編集 (行)
            if (mouseX >= x && mouseX <= x + width - 22 && mouseY >= currentY && mouseY <= currentY + itemHeight) {
                startEditing(index, item, currentY)
                return true
            }
            currentY += itemHeight
        }

        // 追加ボタン判定 (リストの末尾)
        if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + 20) {
            startEditing(-1, null, currentY)
            return true
        }

        return super.mouseClicked(mouseButtonEvent, bl)
    }

    private fun startEditing(index: Int, item: T?, atY: Int) {
        editingIndex = index
        activeInputWidget = property.createInputWidget(x + 5, atY, width - 10, item) { newValue ->
            if (newValue != null) {
                if (editingIndex == -1) property.add(newValue) else property.replaceAt(editingIndex, newValue)
            }
            activeInputWidget = null
            editingIndex = -1
        }
    }

    override fun renderWidget(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        super.renderWidget(guiGraphics, i, j, f)
        val colorScheme = InfiniteClient.theme.colorScheme
        val font = Minecraft.getInstance().font

        var currentY = y + DEFAULT_WIDGET_HEIGHT
        val itemHeight = 20

        // 枠内背景（リスト領域を視覚化）
        guiGraphics.fill(
            x,
            y + DEFAULT_WIDGET_HEIGHT,
            x + width,
            y + height,
            colorScheme.backgroundColor.mix(colorScheme.secondaryColor, 0.1f),
        )

        property.value.forEachIndexed { index, item ->
            if (index == editingIndex) {
                activeInputWidget?.y = currentY // 位置を同期
                currentY += itemHeight
                return@forEachIndexed
            }

            val isHover = i >= x && i <= x + width && j >= currentY && j <= currentY + itemHeight
            if (isHover) {
                guiGraphics.fill(
                    x,
                    currentY,
                    x + width,
                    (currentY + itemHeight),
                    colorScheme.secondaryColor,
                )
            }

            guiGraphics.drawString(
                font,
                property.elementToString(item),
                x + 5,
                currentY + 5,
                colorScheme.foregroundColor,
            )
            guiGraphics.drawString(font, "×", x + width - 15, currentY + 5, colorScheme.accentColor)

            currentY += itemHeight
        }

        activeInputWidget?.render(guiGraphics, i, j, f)

        // 追加ボタンの描画
        if (activeInputWidget == null || editingIndex != -1) {
            val isHoverAdd = i >= x && i <= x + width && j >= currentY && j <= currentY + 20
            val color = if (isHoverAdd) colorScheme.accentColor else colorScheme.secondaryColor
            guiGraphics.fill(x, currentY, x + width, (currentY + 20), color)
            guiGraphics.drawString(
                font,
                "+ Add",
                x + (width - font.width("+ Add")) / 2,
                currentY + 5,
                colorScheme.foregroundColor,
            )
        }
    }

    override fun children(): List<GuiEventListener> {
        // activeInputWidgetを優先的に返す
        return activeInputWidget?.let { listOf(it) } ?: emptyList()
    }

    // キー入力イベントを内部ウィジェットに転送
    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        return activeInputWidget?.keyPressed(keyEvent) ?: super.keyPressed(keyEvent)
    }

    // 文字入力イベントを内部ウィジェットに転送
    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        return activeInputWidget?.charTyped(characterEvent) ?: super.charTyped(characterEvent)
    }
}
