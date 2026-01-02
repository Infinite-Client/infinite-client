package org.infinite.infinite.ui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.graphics.graphics2d.text.IModernFontManager
import org.infinite.libs.graphics.text.fromFontSet
import org.infinite.libs.ui.layout.RenderableScrollableLayout
import org.infinite.libs.ui.screen.AbstractCarouselScreen
import org.infinite.libs.ui.widgets.AbstractCarouselWidget
import org.infinite.mixin.graphics.MinecraftAccessor
import kotlin.math.roundToInt

class LocalCategoryWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val category: LocalCategory,
    parent: AbstractCarouselScreen<LocalCategory>,
    index: Int,
) : AbstractCarouselWidget<LocalCategory>(
    x,
    y,
    width,
    height,
    category,
    parent,
    index,
    Component.translatable(category.translation()),
) {
    private data class WidgetComponents(
        val titleComponent: StringWidget,
        val container: RenderableScrollableLayout,
    )

    private val widgetComponents: WidgetComponents

    init {
        val minecraft = Minecraft.getInstance()
        val minecraftAccessor = minecraft as MinecraftAccessor
        val fontManager = minecraftAccessor.fontManager as IModernFontManager
        val fontSet = fontManager.`infinite$fontSetFromIdentifier`("infinite_regular")
        val font = fromFontSet(fontSet)
        val titleText = Component.translatable(category.translation())
        val width = parent.widgetWidth.roundToInt()
        val titleComponentWidth = font.width(titleText)
        val titleComponentHeight = font.lineHeight
        val titleY = titleComponentHeight * 2
        val widgetHeight = font.lineHeight
        val title = StringWidget(
            (width - titleComponentWidth) / 2,
            titleY,
            font.width(titleText),
            widgetHeight,
            titleText,
            font,
        )
        val containerMargin = 10
        val widgetWidth = width - containerMargin
        val scrollY = titleY + font.lineHeight + containerMargin
        val scrollHeight = widgetHeight - scrollY - containerMargin
        val innerLayout = LinearLayout.vertical().spacing(5)
        val container = RenderableScrollableLayout(minecraft, innerLayout, widgetWidth)
        val containerHeight = height - scrollY - containerMargin
        container.y = scrollY
        container.setMaxHeight(containerHeight)
        container.setMinWidth(widgetWidth)
        container.x = containerMargin
        // 内部の縦並びレイアウト

        // 仮定: category.features に含まれる各機能をボタンとして追加
        // 実際のプロパティ名に合わせて調整してください
        category.features.forEach { (key, feature) ->
            val c = Component.translatable(feature.translation())
            val button = Button.builder(c) {
                println("Feature Clicked: ${c.string}")
            }.size(widgetWidth - 20, 20).build()
            innerLayout.addChild(button)
        }
        innerLayout.arrangeElements()
        widgetComponents = WidgetComponents(title, container)
    }

    private val spawnTime = System.currentTimeMillis()
    private val animationDuration = 500L
    private val thisPageProgress = thisIndex.toFloat() / parent.pageSize

    init {
        addInnerWidget(widgetComponents.titleComponent)
        addInnerWidget(widgetComponents.container)
    }

    override fun render(graphics2D: AbstractCarouselScreen.WidgetGraphics2D): AbstractCarouselScreen.WidgetGraphics2D {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val alpha = ((System.currentTimeMillis() - spawnTime).toFloat() / animationDuration * 0.5f).coerceIn(0f, 0.5f)
        val width =
            graphics2D.width.toFloat()
        val height =
            graphics2D.height.toFloat()
        theme.renderBackGround(
            0f,
            0f,
            width,
            height,
            graphics2D,
            alpha,
        )
        graphics2D.strokeStyle.width = 2f
        val startColor = colorScheme.color(360 * thisPageProgress, 1f, 0.5f, alpha)
        val endColor = colorScheme.color(360 * (thisPageProgress + 0.5f / parent.pageSize), 1f, 0.5f, alpha)
        graphics2D.strokeRect(0f, 0f, width, height, startColor, startColor, endColor, endColor)
        return graphics2D
    }

    override fun onSelected(data: LocalCategory) {
        println("Selected: ${data.translation()}")
    }
}
