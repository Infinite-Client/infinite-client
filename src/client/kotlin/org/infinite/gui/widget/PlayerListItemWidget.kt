package org.infinite.gui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D

class PlayerListItemWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int, // This height is for the individual item
    private val playerName: String,
    private val onRemove: (String) -> Unit,
) : AbstractWidget(x, y, width, height, Component.literal(playerName)) {
    private val padding = 8
    private val removeButtonWidth = 20

    override fun renderWidget(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, Minecraft.getInstance().deltaTracker)

        val textX = x + padding
        val textY = y + this.height / 2 - 4
        graphics2D.drawText(
            Component.literal(playerName),
            textX,
            textY,
            InfiniteClient
                .currentColors()
                .foregroundColor,
            true, // shadow = true
        )

        val removeButtonX = x + width - padding - removeButtonWidth
        val removeButtonY = y
        val isRemoveButtonHovered =
            mouseX >= removeButtonX &&
                mouseX < removeButtonX + removeButtonWidth &&
                mouseY >= removeButtonY &&
                mouseY < removeButtonY + this.height

        val baseColor =
            InfiniteClient
                .currentColors()
                .errorColor
        val hoverColor =
            InfiniteClient
                .currentColors()
                .errorColor

        val removeColor = if (isRemoveButtonHovered) hoverColor else baseColor

        context.fill(
            removeButtonX,
            removeButtonY,
            removeButtonX + removeButtonWidth,
            removeButtonY + this.height,
            removeColor,
        )
        graphics2D.drawText(
            "x",
            removeButtonX + removeButtonWidth / 2 - 3,
            removeButtonY + this.height / 2 - 4,
            InfiniteClient
                .currentColors()
                .foregroundColor,
            false, // shadow = false
        )
    }

    /**
     * 新しいClickableWidgetに合わせて mouseClicked のシグネチャを修正しました。
     * 座標は Click オブジェクトから取得し、ボタンは buttonInfo().button() で確認します。
     */
    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        // マウスの座標を取得
        val mouseX = click.x()
        val mouseY = click.y()
        // 左クリック（ボタン0）でのみ処理を実行
        if (click.buttonInfo().button() == 0) {
            val removeButtonX = x + width - padding - removeButtonWidth
            val removeButtonY = y

            if (mouseX >= removeButtonX &&
                mouseX < removeButtonX + removeButtonWidth &&
                mouseY >= removeButtonY &&
                mouseY < removeButtonY + this.height
            ) {
                // ボタンクリック音を鳴らす
                this.playDownSound(Minecraft.getInstance().soundManager)
                onRemove(playerName)
                return true
            }
        }

        // それ以外の場合は親クラスの処理を呼び出す（ウィジェット全体のクリック処理など）
        return super.mouseClicked(click, doubled)
    }

    // keyPressed と charTyped は ClickableWidget の Element インターフェースのメソッドですが、
    // ClickableWidget の実装を継承しているため、ここでは削除またはコメントアウトします。
    // 新しい ClickableWidget ではこれらのメソッドをオーバーライドする必要性は低いですが、
    // エラーが出ている場合は残してください。ただし、ClickableWidgetが提供する Element の
    // 実装をそのまま利用することが推奨されます。

    // override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = super.keyPressed(keyCode, scanCode, modifiers)
    // override fun charTyped(chr: Char, modifiers: Int): Boolean = super.charTyped(chr, modifiers)

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        // デフォルトのナレーションを追加してから、追加の情報を付与
        this.defaultButtonNarrationText(builder)
        builder.add(NarratedElementType.TITLE, Component.literal("Player List Item: $playerName"))
    }
}
