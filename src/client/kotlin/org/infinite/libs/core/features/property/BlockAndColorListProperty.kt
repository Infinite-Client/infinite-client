package org.infinite.libs.core.features.property

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.EditBox
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.log.LogSystem
import org.infinite.libs.ui.widgets.SuggestInputWidget
import org.infinite.utils.alpha
import org.infinite.utils.mix
import org.lwjgl.glfw.GLFW
import kotlin.jvm.optionals.getOrNull

data class BlockAndColor(
    val blockId: String,
    val color: Int, // AARRGGBB
)

class BlockAndColorListProperty(default: List<BlockAndColor>) : ListProperty<BlockAndColor>(default) {

    override fun convertElement(anyValue: Any): BlockAndColor? {
        if (anyValue is BlockAndColor) return anyValue
        if (anyValue is String) {
            val parts = anyValue.split("#")
            if (parts.size == 2) {
                val blockId = parts[0]
                val colorString = parts[1]
                val color = try {
                    colorString.toLong(16).toInt() // AARRGGBB
                } catch (e: NumberFormatException) {
                    LogSystem.error("Invalid color format: $colorString")
                    0xFFFFFFFF.toInt() // デフォルトは白
                }
                return BlockAndColor(blockId, color)
            } else if (parts.size == 1) {
                // 色が指定されていない場合はデフォルト色
                return BlockAndColor(parts[0], 0xFFFFFFFF.toInt())
            }
        }
        return null
    }

    override fun renderElement(graphics2D: Graphics2D, item: BlockAndColor, x: Int, y: Int, width: Int, height: Int) {
        // ブロックIDの描画
        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.textStyle.size = height - 4f
        graphics2D.textStyle.shadow = true
        graphics2D.fillStyle = InfiniteClient.theme.colorScheme.foregroundColor
        graphics2D.text(item.blockId, x + 24, y + (height - 8) / 2)

        // ブロックアイコンの描画
        val identifier = Identifier.tryParse(item.blockId)
        val block = if (identifier != null) BuiltInRegistries.BLOCK.get(identifier).getOrNull()?.value() else null
        if (block != null) {
            val blockSize = height - 2f
            graphics2D.blockCentered(block, x + height / 2f, y + height / 2f, blockSize)
        }

        // カラーボックスの描画
        val boxSize = 12f
        graphics2D.fillStyle = item.color.alpha(255) // アルファ値を255に固定して描画
        graphics2D.fillRect(x + width - boxSize - 4f, y + (height - boxSize) / 2f, boxSize, boxSize)
    }

    override fun createInputWidget(
        x: Int,
        y: Int,
        width: Int,
        initialValue: BlockAndColor?,
        onComplete: (BlockAndColor?) -> Unit,
    ): AbstractWidget {
        val initialBlockId = initialValue?.blockId ?: ""
        val initialColor = initialValue?.color?.let { "#%08X".format(it) } ?: "#FFFFFFFF"

        val editBox = object : EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.literal("")) {
            init {
                value = "$initialBlockId$initialColor"
                isFocused = true
                cursorPosition = this.value.length
                setResponder { newValue ->
                    // リアルタイムでサジェストを更新するために必要であればここにロジックを追加
                }
            }

            override fun keyPressed(keyEvent: net.minecraft.client.input.KeyEvent): Boolean {
                if (keyEvent.key == GLFW.GLFW_KEY_ENTER) {
                    onComplete(convertElement(this.value))
                    return true
                }
                if (keyEvent.key == GLFW.GLFW_KEY_ESCAPE) {
                    onComplete(null)
                    return true
                }
                return super.keyPressed(keyEvent)
            }
        }

        // サジェスト機能付きのEditBoxをブロックID部分に適用
        val blockIdEditBox = SuggestInputWidget(
            x,
            y,
            width - 60, // 色入力用のスペースを確保
            initialBlockId,
            suggestions = { BuiltInRegistries.BLOCK.keySet().map { it.toString() } },
            onComplete = { newBlockId ->
                if (newBlockId != null) {
                    val currentColor = try {
                        editBox.value.split("#").getOrElse(1) { "FFFFFFFF" }
                    } catch (e: Exception) {
                        "FFFFFFFF"
                    }
                    onComplete(BlockAndColor(newBlockId, currentColor.toLong(16).toInt()))
                } else {
                    onComplete(null)
                }
            },
        ).apply {
            setX(x)
            setY(y)
            setWidth(width - 60)
            value = initialBlockId
            isFocused = true // 常にフォーカスされるようにする
        }

        val colorEditBox = EditBox(
            Minecraft.getInstance().font,
            x + width - 55, // ブロックID入力欄の隣
            y,
            55, // #AARRGGBB で9文字なので、余裕を持たせる
            20,
            Component.literal(""),
        ).apply {
            value = initialColor
            setResponder { newColor ->
                // ここでは色のプレビューなどを実装可能
            }
            // TabキーでブロックID入力欄とのフォーカス移動を可能にする
        }

        // このListPropertyWidgetが返すのはEditBox全体ではなく、
        // サジェスト入力と色入力用の両方をカバーする複合的なウィジェットにする必要がある。
        // しかし、PropertyWidgetのcreateInputWidgetはAbstractWidgetを1つしか返せないので、
        // 現状はメインのSuggestInputWidgetを返し、色はSuggestInputWidget内で処理する形に変更します。
        // もしくは、CustomInputWidgetを定義してその中で複数の要素を管理します。

        // 一旦、元の実装のまま、ブロックIDと色の文字列を一つのEditBoxで管理する形に戻します。
        // #AARRGGBB形式はEditBoxのサジェストに馴染まないので、ユーザーが手動で入力する形です。
        // ブロックIDのサジェストは、EditBoxのvalueが変更されたときにトリガーされるようにします。

        return object : EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.literal("")) {
            init {
                value = initialValue?.let { "${it.blockId}#%08X".format(it.color) } ?: ""
                isFocused = true
                cursorPosition = this.value.length
                setResponder { input ->
                    // ここでサジェストを更新する
                    val currentBlockId = input.split("#").firstOrNull() ?: input
                    updateSuggestionsForEditBox(currentBlockId)
                }
            }

            private var currentSuggestions = emptyList<String>()
            private var currentSelectedIndex = -1

            private fun updateSuggestionsForEditBox(input: String) {
                currentSuggestions = if (input.isEmpty()) {
                    emptyList()
                } else {
                    BuiltInRegistries.BLOCK.keySet().map { it.toString() }
                        .filter { it.contains(input, ignoreCase = true) }
                        .take(5)
                }
                currentSelectedIndex = -1
            }

            override fun keyPressed(keyEvent: net.minecraft.client.input.KeyEvent): Boolean {
                val keyCode = keyEvent.key
                if (keyCode == GLFW.GLFW_KEY_ENTER) {
                    val finalValue = if (currentSelectedIndex != -1) currentSuggestions[currentSelectedIndex] + "#" + this.value.split("#").getOrElse(1) { "FFFFFFFF" } else this.value
                    onComplete(convertElement(finalValue))
                    return true
                }
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    onComplete(null)
                    return true
                }
                if (keyCode == GLFW.GLFW_KEY_TAB && currentSuggestions.isNotEmpty()) {
                    val isShiftDown = keyEvent.modifiers and GLFW.GLFW_MOD_SHIFT != 0
                    currentSelectedIndex = if (isShiftDown) {
                        (currentSelectedIndex - 1 + currentSuggestions.size) % currentSuggestions.size
                    } else {
                        (currentSelectedIndex + 1) % currentSuggestions.size
                    }
                    this.value = currentSuggestions[currentSelectedIndex] + "#" + this.value.split("#").getOrElse(1) { "FFFFFFFF" }
                    return true
                }
                return super.keyPressed(keyEvent)
            }

            override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
                super.renderWidget(guiGraphics, mouseX, mouseY, delta)

                // サジェストの描画
                if (isFocused && currentSuggestions.isNotEmpty()) {
                    val theme = InfiniteClient.theme
                    val colorScheme = theme.colorScheme
                    val g2d = Graphics2DRenderer(guiGraphics)

                    var suggestY = (y + height + 2).toFloat()
                    val entryHeight = 14f

                    for (index in currentSuggestions.indices) {
                        val suggestion = currentSuggestions[index]
                        val isSelected = index == currentSelectedIndex

                        val alpha = 0.9f
                        theme.renderBackGround(x.toFloat(), suggestY, width.toFloat(), entryHeight, g2d, alpha)

                        if (isSelected) {
                            g2d.fillStyle = colorScheme.accentColor.mix(0x00000000, 0.7f)
                            g2d.fillRect(x.toFloat(), suggestY, width.toFloat(), entryHeight)
                            g2d.fillStyle = colorScheme.accentColor
                            g2d.fillRect(x.toFloat(), suggestY, 2f, entryHeight)
                        }

                        g2d.textStyle.size = 10f
                        g2d.textStyle.font = "infinite_regular"
                        g2d.fillStyle = if (isSelected) colorScheme.foregroundColor else colorScheme.secondaryColor

                        g2d.text(suggestion, x + 4f, suggestY + (entryHeight / 2f) - 4f)
                        suggestY += entryHeight
                    }
                    g2d.flush()
                }
            }
        }
    }
}
