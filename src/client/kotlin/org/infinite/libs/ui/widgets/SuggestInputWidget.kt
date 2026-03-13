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
        this.isBordered = false
        this.isFocused = true
    }

    private fun updateSuggestions(input: String) {
        val allSuggestions = suggestions()

        filteredSuggestions = if (input.isBlank()) {
            mutableListOf()
        } else {
            allSuggestions
                .filter { suggestion ->
                    suggestion.startsWith(input, ignoreCase = true) ||
                        suggestion.contains(input, ignoreCase = true)
                }
                .sortedBy { !it.startsWith(input, ignoreCase = true) } // 前方一致を先頭に
                .toMutableList()
        }

        // 入力が変わったら選択状態をリセット（Googleライク）
        selectedIndex = -1
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.key
        val modifiers = keyEvent.modifiers

        // Tab / Shift+Tab で候補を循環選択
        if (keyCode == GLFW.GLFW_KEY_TAB && filteredSuggestions.isNotEmpty()) {
            val isShiftDown = (modifiers and GLFW.GLFW_MOD_SHIFT) != 0

            // 循環処理
            if (isShiftDown) {
                selectedIndex--
                if (selectedIndex < -1) {
                    selectedIndex = filteredSuggestions.size - 1
                }
            } else {
                selectedIndex++
                if (selectedIndex >= filteredSuggestions.size) {
                    selectedIndex = -1
                }
            }

            if (selectedIndex >= 0) {
                this.value = filteredSuggestions[selectedIndex]
                this.cursorPosition = this.value.length
                selectedIndex = -1 // ← これを追加すると補完後は選択解除
            } else {
                // 選択解除（-1）のときは現在の入力値を維持（ユーザーが自分で入力したまま）
                // 特に何もしないのが自然
            }

            return true
        }

        // Enter で確定
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            val finalValue = when {
                selectedIndex >= 0 -> filteredSuggestions[selectedIndex]
                else -> this.value.trim()
            }

            if (finalValue.isNotBlank()) {
                onComplete(finalValue)
            } else {
                onComplete(null) // 空ならキャンセル扱いでも良い（好み）
            }
            return true
        }

        // Esc でキャンセル
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onComplete(null)
            return true
        }

        // その他のキー（文字入力、Backspace、矢印など）は通常処理へ
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
