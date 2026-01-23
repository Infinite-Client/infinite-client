package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.mix
import org.lwjgl.glfw.GLFW

/**
 * サジェスト機能付きのEditBox (Graphics2D 対応版)
 */
open class SuggestInputWidget(
    x: Int,
    y: Int,
    width: Int,
    initialValue: String,
    val suggestions: () -> List<String>,
    val onComplete: (String?) -> Unit,
) : EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.literal("")) {

    private var filteredSuggestions = mutableListOf<String>()
    private var selectedIndex = -1

    init {
        this.value = initialValue
        this.setResponder { updateSuggestions(it) }
        this.setBordered(false)
        this.isFocused = true
    }

    private fun updateSuggestions(input: String) {
        val all = suggestions()
        filteredSuggestions = if (input.isEmpty()) {
            mutableListOf()
        } else {
            all.filter { it.contains(input, ignoreCase = true) }.take(5).toMutableList()
        }
        selectedIndex = -1
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.key

        if (keyCode == GLFW.GLFW_KEY_TAB && filteredSuggestions.isNotEmpty()) {
            val isShiftDown = keyEvent.modifiers and GLFW.GLFW_MOD_SHIFT != 0
            if (isShiftDown) {
                selectedIndex--
                if (selectedIndex < -1) selectedIndex = filteredSuggestions.size - 1
            } else {
                selectedIndex++
                if (selectedIndex >= filteredSuggestions.size) selectedIndex = -1
            }
            return true
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            val finalValue = if (selectedIndex != -1) filteredSuggestions[selectedIndex] else this.value
            if (finalValue.isNotBlank()) onComplete(finalValue)
            return true
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onComplete(null)
            return true
        }

        return super.keyPressed(keyEvent)
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // 本体の入力ボックスを描画 (Minecraft標準の描画を維持)
        super.renderWidget(guiGraphics, mouseX, mouseY, delta)

        if (isFocused && filteredSuggestions.isNotEmpty()) {
            val theme = InfiniteClient.theme
            val colorScheme = theme.colorScheme

            // Graphics2D のセットアップ
            val g2d = Graphics2DRenderer(guiGraphics)

            var suggestY = (y + height + 2).toFloat()
            val entryHeight = 14f

            for (index in filteredSuggestions.indices) {
                val suggestion = filteredSuggestions[index]
                val isSelected = index == selectedIndex

                // --- 1. 背景描画 (Theme API) ---
                // 選択中（Tab選択）はアクセントカラーを混ぜる
                val alpha = 0.9f
                theme.renderBackGround(x.toFloat(), suggestY, width.toFloat(), entryHeight, g2d, alpha)

                if (isSelected) {
                    g2d.fillStyle = colorScheme.accentColor.mix(0x00000000, 0.7f)
                    g2d.fillRect(x.toFloat(), suggestY, width.toFloat(), entryHeight)

                    // 左側にインジケーター（枠線）を表示
                    g2d.fillStyle = colorScheme.accentColor
                    g2d.fillRect(x.toFloat(), suggestY, 2f, entryHeight)
                }

                // --- 2. テキスト描画 ---
                g2d.textStyle.size = 10f
                g2d.textStyle.font = "infinite_regular"
                g2d.fillStyle = if (isSelected) colorScheme.foregroundColor else colorScheme.secondaryColor

                // 文字の描画 (左詰めでパディング 4px)
                g2d.text(suggestion, x + 4f, suggestY + (entryHeight / 2f) - 4f)

                suggestY += entryHeight
            }

            // 全体のフラッシュ
            g2d.flush()
        }
    }
}
