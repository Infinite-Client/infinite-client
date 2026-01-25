package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Property
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.ListProperty
import org.infinite.libs.core.features.property.NumberProperty
import org.infinite.libs.core.features.property.SelectionProperty
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha

open class PropertyWidget<T : Property<*>>(
    x: Int,
    y: Int,
    width: Int,
    height: Int = DEFAULT_WIDGET_HEIGHT,
    protected val property: T,
) : AbstractContainerWidget(x, y, width, height, Component.literal("")),
    Renderable {

    companion object {
        protected const val DEFAULT_WIDGET_HEIGHT = 20
    }

    // テーマ取得用ヘルパー
    protected val themeScheme get() = InfiniteClient.theme.colorScheme
    protected val mcFont: Font
        get() = Minecraft.getInstance().font

    override fun contentHeight(): Int = this.height
    override fun scrollRate(): Double = 10.0
    override fun children(): List<GuiEventListener> = listOf()

    // レイアウト更新用（オーバーライドはそのまま保持）
    override fun setWidth(i: Int) {
        super.setWidth(i)
        relocate()
    }

    override fun setHeight(i: Int) {
        super.setHeight(i)
        relocate()
    }

    override fun setX(i: Int) {
        super.setX(i)
        relocate()
    }

    override fun setY(i: Int) {
        super.setY(i)
        relocate()
    }

    override fun setSize(i: Int, j: Int) {
        super.setSize(i, j)
        relocate()
    }

    override fun setPosition(i: Int, j: Int) {
        super.setPosition(i, j)
        relocate()
    }

    override fun setRectangle(i: Int, j: Int, k: Int, l: Int) {
        super.setRectangle(i, j, k, l)
        relocate()
    }

    protected open fun relocate() {}

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val g2d = Graphics2DRenderer(guiGraphics)

        // 翻訳と説明文の構築
        val translationKey = property.translationKey()
        val rawDescription = translationKey?.let {
            val trans = Component.translatable(it).string
            if (trans != it) trans else ""
        } ?: ""

        val descriptionText = buildDescription(rawDescription, property)
        val name = property.name

        val lineHeight = mcFont.lineHeight.toFloat()
        val padding = 2f

        // 1. プロパティ名の描画
        g2d.textStyle.apply {
            size = lineHeight
            shadow = false
            font = "infinite_regular" // カスタムフォントを使用
        }
        g2d.fillStyle = themeScheme.foregroundColor
        g2d.text(name, x, y)

        // 2. 説明文の描画 (存在する場合)
        if (descriptionText.isNotBlank()) {
            g2d.textStyle.size = lineHeight - 1f // 少し小さく
            g2d.fillStyle = themeScheme.secondaryColor.alpha(180) // 少し透明度を下げてコントラストを調整
            g2d.text(descriptionText, x.toFloat(), y + lineHeight + padding)
        }

        g2d.flush()
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }

    private fun buildDescription(raw: String, property: Property<*>): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        // 文末記号がある場合はそのまま返す
        if (trimmed.any { it == '.' || it == '!' || it == '?' }) return trimmed

        val suffix = "."
        return when (property) {
            is BooleanProperty -> {
                val verbs = listOf("Allow", "Show", "Keep", "Ignore", "Release", "Pause", "Target", "Use")
                if (verbs.any { trimmed.startsWith(it, ignoreCase = true) }) {
                    trimmed + suffix
                } else {
                    "Toggle $trimmed$suffix"
                }
            }

            is SelectionProperty<*> -> "Select $trimmed$suffix"

            is NumberProperty<*> -> "Adjust $trimmed$suffix"

            is ListProperty<*> -> "Edit $trimmed$suffix"

            else -> "Set $trimmed$suffix"
        }
    }
}
