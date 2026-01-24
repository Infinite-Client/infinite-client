package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.property.StringProperty

class StringPropertyWidget(
    x: Int,
    y: Int,
    width: Int,
    property: StringProperty,
) : PropertyWidget<StringProperty>(
    x,
    y,
    width,
    DEFAULT_WIDGET_HEIGHT * 2,
    property,
) {
    // Minecraft標準のテキスト入力ウィジェット
    private val editBox = EditBox(
        Minecraft.getInstance().font,
        x,
        y + DEFAULT_WIDGET_HEIGHT, // 1段下に配置
        width,
        DEFAULT_WIDGET_HEIGHT,
        Component.empty(),
    ).apply {
        setMaxLength(256)
        setBordered(false)
        value = property.value
        setResponder { newValue ->
            property.value = newValue
        }
        // Infiniteのテーマカラーに合わせる（枠線の色など）
        val colorScheme = InfiniteClient.theme.colorScheme
        setTextColor(colorScheme.foregroundColor)
    }

    override fun children(): List<GuiEventListener> = listOf(editBox)

    override fun relocate() {
        super.relocate()
        editBox.x = x
        editBox.y = y + DEFAULT_WIDGET_HEIGHT
        editBox.width = width
        // 高さやその他のスタイル調整が必要な場合はここで行う
    }

    override fun renderWidget(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        // PropertyWidget の基本描画（タイトル/ラベル）
        super.renderWidget(guiGraphics, i, j, f)

        // テキストボックスの描画
        editBox.render(guiGraphics, i, j, f)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean = editBox.charTyped(characterEvent) || super.charTyped(characterEvent)

    override fun keyPressed(keyEvent: KeyEvent): Boolean = editBox.keyPressed(keyEvent) || super.keyPressed(keyEvent)
}
