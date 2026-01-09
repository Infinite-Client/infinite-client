package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer

/**
 * Featureを表示・操作するための基本ウィジェット
 */
abstract class FeatureWidget<T : Feature>(
    x: Int,
    y: Int,
    width: Int,
    height: Int = FONT_SIZE + PADDING * 2,
    protected val feature: T,
) : AbstractContainerWidget(x, y, width, height, Component.literal(feature.name)) {

    companion object {
        const val PADDING = 4
        const val FONT_SIZE = 12
    }

    protected data class WidgetComponents(
        val resetButton: FeatureResetButton,
        val settingButton: FeatureSettingButton,
        val toggleButton: FeatureToggleButton,
    )

    protected val widgetComponents: WidgetComponents

    init {
        val resetButton = FeatureResetButton(0, 0, 0, 0, feature)
        val settingButton = FeatureSettingButton(0, 0, 0, 0, feature)
        val toggleButton = FeatureToggleButton(0, 0, 0, 0, feature)
        widgetComponents = WidgetComponents(resetButton, settingButton, toggleButton)
        relocateChildren()
    }

    private fun relocateChildren() {
        val buttonSize = this.height - PADDING
        val buttonY = this.y + PADDING / 2

        widgetComponents.resetButton.apply {
            x = this@FeatureWidget.x + this@FeatureWidget.width - 4 * PADDING - 4 * buttonSize
            y = buttonY
            width = buttonSize
            height = buttonSize
        }

        widgetComponents.settingButton.apply {
            x = this@FeatureWidget.x + this@FeatureWidget.width - 3 * PADDING - 3 * buttonSize
            y = buttonY
            width = buttonSize
            height = buttonSize
        }

        widgetComponents.toggleButton.apply {
            x = this@FeatureWidget.x + this@FeatureWidget.width - PADDING - 2 * buttonSize
            y = buttonY
            width = 2 * buttonSize
            height = buttonSize
        }
    }

    // 各種座標更新メソッド
    override fun setX(x: Int) {
        super.setX(x)
        relocateChildren()
    }

    override fun setY(y: Int) {
        super.setY(y)
        relocateChildren()
    }

    override fun setPosition(i: Int, j: Int) {
        super.setPosition(i, j)
        relocateChildren()
    }

    override fun setSize(i: Int, j: Int) {
        super.setSize(i, j)
        relocateChildren()
    }

    override fun setWidth(i: Int) {
        super.setWidth(i)
        relocateChildren()
    }

    override fun setHeight(i: Int) {
        super.setHeight(i)
        relocateChildren()
    }

    override fun renderWidget(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        val theme = InfiniteClient.theme
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)

        // 背景描画
        theme.renderBackGround(this.x, this.y, this.width, this.height, graphics2DRenderer, 0.8f)

        // テキスト描画 (名称の描画)
        val colorScheme = theme.colorScheme
        graphics2DRenderer.textStyle.apply {
            font = "infinite_regular"
            size = FONT_SIZE.toFloat()
        }
        graphics2DRenderer.fillStyle = colorScheme.foregroundColor

        // 描画テキストはサブクラスでカスタマイズ可能にする
        val displayName = getDisplayName()
        graphics2DRenderer.text(displayName, (this.x + PADDING).toFloat(), (this.y + PADDING).toFloat())

        // ボタンのレンダリング
        renderButtons(graphics2DRenderer)

        graphics2DRenderer.flush()
    }

    /**
     * ボタン部分の描画。必要に応じてオーバーライド可能。
     */
    protected open fun renderButtons(renderer: Graphics2DRenderer) {
        widgetComponents.resetButton.render(renderer)
        widgetComponents.settingButton.render(renderer)
        widgetComponents.toggleButton.render(renderer)
    }

    /**
     * 表示する名前を決定します。
     */
    protected open fun getDisplayName(): String = feature.name

    override fun children(): List<GuiEventListener> =
        listOf(widgetComponents.resetButton, widgetComponents.settingButton, widgetComponents.toggleButton)

    override fun contentHeight(): Int = height
    override fun scrollRate(): Double = 10.0
    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }
}
