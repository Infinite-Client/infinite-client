package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
import org.infinite.utils.mix

/**
 * Featureを表示・操作するための基本ウィジェット
 */
abstract class ListFeatureWidget<T : Feature>(
    x: Int,
    y: Int,
    width: Int,
    height: Int = FONT_SIZE + PADDING * 2 + 6,
    protected val feature: T,
) : AbstractContainerWidget(x, y, width, height, Component.literal(feature.name)) {

    companion object {
        const val PADDING = 8
        const val FONT_SIZE = 13
        private const val BADGE_SCALE = 0.6f
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
        val buttonSize = this.height - PADDING * 2
        val buttonY = this.y + PADDING

        widgetComponents.resetButton.apply {
            x = this@FeatureWidget.x + this@FeatureWidget.width - 3 * PADDING - 3 * buttonSize
            y = buttonY
            width = buttonSize
            height = buttonSize
        }

        widgetComponents.settingButton.apply {
            x = this@FeatureWidget.x + this@FeatureWidget.width - 2 * PADDING - 2 * buttonSize
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
        val colorScheme = theme.colorScheme
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = (h * 0.35f).coerceAtLeast(6f)

        val baseColor = colorScheme.surfaceColor.mix(colorScheme.backgroundColor, 0.6f)
        val highlight = if (feature.isEnabled()) colorScheme.accentColor else colorScheme.secondaryColor
        val hoverBoost = if (isHovered) 0.12f else 0.0f
        graphics2DRenderer.fillStyle = baseColor.mix(highlight, 0.08f + hoverBoost).alpha(210)
        graphics2DRenderer.fillRoundedRect(x.toFloat(), y.toFloat(), w, h, radius)

        // 描画テキストはサブクラスでカスタマイズ可能にする
        val displayName = getDisplayName()
        val badgeSize = (h - PADDING * 2f) * BADGE_SCALE
        val badgeX = x.toFloat() + PADDING.toFloat()
        val badgeY = y.toFloat() + (h - badgeSize) / 2f

        val levelColor = when (feature.featureType) {
            Feature.FeatureLevel.Cheat -> colorScheme.redColor
            Feature.FeatureLevel.Extend -> colorScheme.yellowColor
            Feature.FeatureLevel.Utils -> colorScheme.greenColor
        }
        graphics2DRenderer.fillStyle = levelColor.alpha(200)
        graphics2DRenderer.fillRoundedRect(badgeX, badgeY, badgeSize, badgeSize, badgeSize / 2f)

        graphics2DRenderer.textStyle.apply {
            font = "infinite_bolditalic"
            size = (badgeSize * 0.7f).coerceAtLeast(10f)
        }
        graphics2DRenderer.fillStyle = colorScheme.foregroundColor
        graphics2DRenderer.textCentered(
            feature.featureType.name.first().toString(),
            badgeX + badgeSize / 2f,
            badgeY + badgeSize * 0.75f,
        )

        graphics2DRenderer.textStyle.apply {
            font = "infinite_regular"
            size = FONT_SIZE.toFloat()
        }
        graphics2DRenderer.text(
            displayName,
            (badgeX + badgeSize + PADDING.toFloat()),
            (this.y + PADDING + 1).toFloat(),
        )

        val description = featureDescription()
        if (description.isNotBlank()) {
            graphics2DRenderer.textStyle.apply {
                font = "infinite_regular"
                size = (FONT_SIZE - 3).toFloat().coerceAtLeast(9f)
            }
            graphics2DRenderer.fillStyle = colorScheme.secondaryColor
            graphics2DRenderer.text(
                description,
                (badgeX + badgeSize + PADDING.toFloat()),
                (this.y + PADDING + FONT_SIZE + 2).toFloat(),
            )
        }

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

    protected open fun featureDescription(): String {
        val key = feature.translation()
        val translated = Component.translatable(key).string
        return if (translated != key && translated != feature.name) translated else ""
    }

    override fun children(): List<GuiEventListener> = listOf(widgetComponents.resetButton, widgetComponents.settingButton, widgetComponents.toggleButton)

    override fun contentHeight(): Int = height
    override fun scrollRate(): Double = 10.0
    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }
}
