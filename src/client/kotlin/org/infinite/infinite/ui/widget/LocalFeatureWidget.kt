package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
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
    )

    private val widgetComponents: WidgetComponents

    init {
        // 初期化時は一時的な座標で作成
        val resetButton = FeatureResetButton(0, 0, 0, 0, feature)
        val settingButton = FeatureSettingButton(0, 0, 0, 0, feature)
        widgetComponents = WidgetComponents(resetButton, settingButton)
        // 現在の x, y, width, height に基づいて子要素を配置
        relocateChildren()
    }

    /**
     * 親の座標やサイズに基づいて子要素の位置・サイズを再計算する
     */
    private fun relocateChildren() {
        val buttonSize = this.height - PADDING
        val buttonY = this.y + PADDING / 2

        // Reset Button の位置 (右から2番目)
        widgetComponents.resetButton.x = this.x + this.width - 2 * PADDING - 2 * buttonSize
        widgetComponents.resetButton.y = buttonY
        widgetComponents.resetButton.width = buttonSize
        widgetComponents.resetButton.height = buttonSize

        // Setting Button の位置 (一番右)
        widgetComponents.settingButton.x = this.x + this.width - PADDING - buttonSize
        widgetComponents.settingButton.y = buttonY
        widgetComponents.settingButton.width = buttonSize
        widgetComponents.settingButton.height = buttonSize
    }

    // --- 座標変更の検知 ---

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

        graphics2DRenderer.flush()
    }

    override fun onClick(mouseButtonEvent: MouseButtonEvent, bl: Boolean) {
        widgetComponents.resetButton.onClick(mouseButtonEvent, bl)
        widgetComponents.settingButton.onClick(mouseButtonEvent, bl)
        super.onClick(mouseButtonEvent, bl)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        widgetComponents.resetButton.onClick(mouseButtonEvent, bl)
        widgetComponents.settingButton.onClick(mouseButtonEvent, bl)
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        widgetComponents.resetButton.mouseDragged(mouseButtonEvent, d, e)
        widgetComponents.settingButton.mouseDragged(mouseButtonEvent, d, e)
        return super.mouseDragged(mouseButtonEvent, d, e)
    }

    override fun mouseMoved(d: Double, e: Double) {
        widgetComponents.resetButton.mouseMoved(d, e)
        widgetComponents.settingButton.mouseMoved(d, e)
        return super.mouseMoved(d, e)
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        widgetComponents.resetButton.mouseScrolled(d, e, f, g)
        widgetComponents.settingButton.mouseScrolled(d, e, f, g)
        return super.mouseScrolled(d, e, f, g)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        widgetComponents.resetButton.mouseReleased(mouseButtonEvent)
        widgetComponents.settingButton.mouseReleased(mouseButtonEvent)
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun children(): List<GuiEventListener> =
        listOf(widgetComponents.resetButton, widgetComponents.settingButton)

    override fun contentHeight(): Int = height
    override fun scrollRate(): Double = 10.0
    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }
}
