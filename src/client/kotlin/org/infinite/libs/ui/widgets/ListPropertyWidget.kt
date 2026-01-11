package org.infinite.libs.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.property.ListProperty
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
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
    DEFAULT_WIDGET_HEIGHT * 10,
    property,
) {
    private var activeInputWidget: AbstractWidget? = null
    private var editingIndex: Int = -1
    private var scrollAmount = 0.0
    private var isDraggingScrollbar = false // ドラッグ判定用

    private val itemHeight = 22f
    private val headerHeight = DEFAULT_WIDGET_HEIGHT.toFloat()
    private val viewHeight: Float get() = height - headerHeight

    // コンテンツの総高さ（アイテム数 + 追加ボタン1つ分）
    private val contentHeight: Float
        get() = (property.value.size + 1) * itemHeight

    // スクロール可能な最大値（0未満にならないようにする）
    private fun getMaxScroll(): Double = (contentHeight - viewHeight).toDouble().coerceAtLeast(0.0)

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = mouseButtonEvent.x
        val my = mouseButtonEvent.y

        // 1. スクロールバーのドラッグ開始判定
        if (getMaxScroll() > 0 && mx >= x + width - 6 && mx <= x + width && my >= y + headerHeight) {
            isDraggingScrollbar = true
            return true
        }

        // 2. 入力ウィジェット優先
        if (activeInputWidget != null) {
            if (activeInputWidget!!.mouseClicked(mouseButtonEvent, bl)) return true
            activeInputWidget = null
            editingIndex = -1
            return true
        }

        // 3. リスト要素の判定 (表示領域内のみ)
        if (mx >= x && mx <= x + width && my >= y + headerHeight && my <= y + height) {
            var currentY = y + headerHeight - scrollAmount.toFloat()
            property.value.forEachIndexed { index, _ ->
                // 要素ごとの当たり判定
                if (my >= currentY && my <= currentY + itemHeight) {
                    val bx = x + width - 20f
                    if (mx >= bx) { // 削除ボタン
                        property.removeAt(index)
                    } else { // 編集
                        startEditing(index, property.value[index], currentY.toInt())
                    }
                    return true
                }
                currentY += itemHeight
            }
            // 追加ボタン判定
            if (my >= currentY && my <= currentY + itemHeight) {
                startEditing(-1, null, currentY.toInt())
                return true
            }
        }

        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        if (isDraggingScrollbar) {
            val max = getMaxScroll()
            val barAreaHeight = viewHeight
            val relativeY = (mouseButtonEvent.y - (y + headerHeight)).coerceIn(0.0, barAreaHeight.toDouble())
            scrollAmount = (relativeY / barAreaHeight) * max
            return true
        }
        return activeInputWidget?.mouseDragged(mouseButtonEvent, d, e) ?: super.mouseDragged(mouseButtonEvent, d, e)
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        val max = getMaxScroll()
        if (max > 0) {
            scrollAmount = (scrollAmount - g * 16.0).coerceIn(0.0, max)
            return true
        }
        return false
    }

    override fun renderWidget(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        // 背景とタイトルの描画
        super.renderWidget(guiGraphics, i, j, f)

        val g2d = Graphics2DRenderer(guiGraphics)
        val theme = InfiniteClient.theme

        g2d.enableScissor(x, (y + headerHeight).toInt(), width, (height - headerHeight).toInt())

        var currentY = y + headerHeight - scrollAmount.toFloat()

        property.value.forEachIndexed { index, item ->
            if (index == editingIndex) {
                activeInputWidget?.apply {
                    this.y = currentY.toInt() + 1
                    this.render(guiGraphics, i, j, f)
                }
            } else {
                // ホバー判定
                val isHover =
                    i >= x && i <= x + width - 10 && j >= currentY && j <= currentY + itemHeight && j >= y + headerHeight
                if (isHover) {
                    g2d.fillStyle = theme.colorScheme.secondaryColor.mix(theme.colorScheme.backgroundColor, 0.4f)
                    g2d.fillRect(x.toFloat(), currentY, width.toFloat() - 6f, itemHeight)
                }
                property.renderElement(g2d, item, x + 4, currentY.toInt() + 2, width - 35, itemHeight.toInt() - 4)
                renderRemoveButton(g2d, currentY, i, j)
            }
            currentY += itemHeight
        }

        // 追加ボタン
        if (editingIndex != -1 || activeInputWidget == null) {
            renderAddButton(g2d, currentY, i, j)
        } else {
            activeInputWidget?.apply {
                this.y = currentY.toInt() + 1
                this.render(guiGraphics, i, j, f)
            }
        }
        g2d.disableScissor()
        g2d.flush()

        renderScrollbar(g2d)
    }

    private fun renderScrollbar(g2d: Graphics2DRenderer) {
        val max = getMaxScroll()
        if (max <= 0) return

        val barWidth = 3f
        val barX = x + width - barWidth - 1f
        val barAreaHeight = viewHeight

        // ノブの高さ (コンテンツ比率に応じるが、最小 20px)
        val knobHeight = (barAreaHeight * (viewHeight / contentHeight)).coerceAtLeast(20f)
        val scrollPercent = (scrollAmount / max).toFloat()
        val knobY = (y + headerHeight) + (barAreaHeight - knobHeight) * scrollPercent

        g2d.fillStyle = InfiniteClient.theme.colorScheme.accentColor.alpha(if (isDraggingScrollbar) 255 else 160)
        g2d.fillRect(barX, knobY, barWidth, knobHeight)
        g2d.flush()
    }

    private fun startEditing(index: Int, item: T?, atY: Int) {
        editingIndex = index
        activeInputWidget = property.createInputWidget(x + 4, atY + 1, width - 8, item) { newValue ->
            if (newValue != null) {
                if (editingIndex == -1) {
                    property.add(newValue)
                } else {
                    property.replaceAt(editingIndex, newValue)
                }
            }
            activeInputWidget = null
            editingIndex = -1
        }.apply {
            this.isFocused = true // キーボード入力を受け付けるために必要
        }
    }

    /**
     * 削除ボタン（バツ印）の描画
     */
    private fun renderRemoveButton(g2d: Graphics2DRenderer, currentY: Float, mouseX: Int, mouseY: Int) {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme

        val btnSize = 12f
        val bx = x + width - btnSize - 6f
        val by = currentY + (itemHeight - btnSize) / 2f

        // ホバー判定（スクロール後の座標で判定）
        val isHoverDel = mouseX >= bx && mouseX <= bx + btnSize && mouseY >= by && mouseY <= by + btnSize

        // ボタンの背景
        g2d.fillStyle = if (isHoverDel) colorScheme.accentColor else colorScheme.secondaryColor
        g2d.fillRect(bx, by, btnSize, btnSize)

        // バツ印の線
        g2d.beginPath()
        g2d.strokeStyle.color = colorScheme.foregroundColor
        g2d.strokeStyle.width = 1.0f
        val p = 3f // 内側パディング

        // 左上から右下
        g2d.moveTo(bx + p, by + p)
        g2d.lineTo(bx + btnSize - p, by + btnSize - p)
        g2d.strokePath()

        // 右上から左下
        g2d.moveTo(bx + btnSize - p, by + p)
        g2d.lineTo(bx + p, by + btnSize - p)
        g2d.strokePath()
    }

    /**
     * リスト末尾の追加ボタンの描画
     */
    private fun renderAddButton(g2d: Graphics2DRenderer, currentY: Float, mouseX: Int, mouseY: Int) {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme

        val buttonHeight = 22f
        // ホバー判定
        val isHoverAdd = mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + buttonHeight

        val opacity = if (isHoverAdd) 1f else 0.5f

        // 背景（テーマの標準的なボタン背景を利用）
        theme.renderBackGround(x.toFloat(), currentY, width.toFloat(), buttonHeight, g2d, opacity)

        // テキスト
        g2d.fillStyle = colorScheme.foregroundColor
        g2d.textCentered("+ Add Item", x + width / 2f, currentY + buttonHeight / 2f)
    }

    // キーボード入力の転送
    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        return activeInputWidget?.keyPressed(keyEvent) ?: super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        return activeInputWidget?.charTyped(characterEvent) ?: super.charTyped(characterEvent)
    }

    // マウス移動・ドラッグの転送（入力欄内のカーソル選択などに必要）
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        activeInputWidget?.mouseMoved(mouseX, mouseY)
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        isDraggingScrollbar = false
        return activeInputWidget?.mouseReleased(mouseButtonEvent) ?: super.mouseReleased(mouseButtonEvent)
    }
}
