package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer

class LocalFeatureWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int = FONT_SIZE + PADDING * 2,
    private val feature: LocalFeature,
) : AbstractContainerWidget(x, y, width, height, Component.literal(feature.name)) {

    companion object {
        const val PADDING = 4
        const val FONT_SIZE = 12
    }

    private data class WidgetComponents(
        val resetButton: FeatureResetButton,
        val settingButton: FeatureSettingButton,
        val toggleButton: FeatureToggleButton,
    )

    private val widgetComponents: WidgetComponents

    init {
        // 初期化時は一時的な座標で作成
        val resetButton = FeatureResetButton(0, 0, 0, 0, feature)
        val settingButton = FeatureSettingButton(0, 0, 0, 0, feature)
        val toggleButton = FeatureToggleButton(0, 0, 0, 0, feature)
        widgetComponents = WidgetComponents(resetButton, settingButton, toggleButton)
        relocateChildren()
    }

    /**
     * 親の座標やサイズに基づいて子要素の位置・サイズを再計算する
     */
    private fun relocateChildren() {
        val buttonSize = this.height - PADDING
        val buttonY = this.y + PADDING / 2

        widgetComponents.resetButton.x = this.x + this.width - 3 * PADDING - 3 * buttonSize
        widgetComponents.resetButton.y = buttonY
        widgetComponents.resetButton.width = buttonSize
        widgetComponents.resetButton.height = buttonSize

        widgetComponents.settingButton.x = this.x + this.width - 2 * PADDING - 2 * buttonSize
        widgetComponents.settingButton.y = buttonY
        widgetComponents.settingButton.width = buttonSize
        widgetComponents.settingButton.height = buttonSize

        widgetComponents.toggleButton.x = this.x + this.width - PADDING - buttonSize
        widgetComponents.toggleButton.y = buttonY
        widgetComponents.toggleButton.width = buttonSize
        widgetComponents.toggleButton.height = buttonSize
    }

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

        // テキスト描画
        val colorScheme = theme.colorScheme
        graphics2DRenderer.textStyle.apply {
            font = "infinite_regular"
            size = FONT_SIZE.toFloat()
        }
        graphics2DRenderer.fillStyle = colorScheme.foregroundColor
        graphics2DRenderer.text(feature.name, (this.x + PADDING).toFloat(), (this.y + PADDING).toFloat())

        widgetComponents.resetButton.render(graphics2DRenderer)
        widgetComponents.settingButton.render(graphics2DRenderer)
        widgetComponents.toggleButton.render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }

    override fun children(): List<GuiEventListener> =
        listOf(widgetComponents.resetButton, widgetComponents.settingButton, widgetComponents.toggleButton)

    override fun contentHeight(): Int = height
    override fun scrollRate(): Double = 10.0
    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }
}
