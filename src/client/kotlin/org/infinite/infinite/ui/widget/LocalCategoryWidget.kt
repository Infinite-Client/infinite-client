package org.infinite.infinite.ui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.graphics.graphics2d.text.IModernFontManager
import org.infinite.libs.graphics.text.fromFontSet
import org.infinite.libs.log.LogSystem
import org.infinite.libs.ui.layout.IScrollableLayout
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
        val container: IScrollableLayout,
    )

    private val widgetComponents: WidgetComponents

    init {
        val minecraft = Minecraft.getInstance()
        val minecraftAccessor = minecraft as MinecraftAccessor
        val fontManager = minecraftAccessor.fontManager as IModernFontManager
        val fontSet = fontManager.`infinite$fontSetFromIdentifier`("infinite_regular")
        val font = fromFontSet(fontSet)
        val width = parent.widgetWidth.roundToInt()
        val titleComponentHeight = font.lineHeight
        val titleY = titleComponentHeight * 2
        val containerMargin = 10
        val widgetWidth = width - 2 * containerMargin
        val scrollY = titleY + font.lineHeight + containerMargin
        val innerSpacing = 5
        val innerLayout = LinearLayout.vertical().spacing(innerSpacing)
        val containerHeight = height - scrollY - containerMargin
        category.features.forEach { (_, feature) ->
            innerLayout.addChild(LocalFeatureWidget(0, 0, widgetWidth - 2 * innerSpacing, feature = feature))
        }
        innerLayout.arrangeElements()
        val container = IScrollableLayout(minecraft, innerLayout, widgetWidth)
        container.y = scrollY
        container.setMaxHeight(containerHeight)
        container.setMinWidth(widgetWidth)
        container.x = containerMargin
        widgetComponents = WidgetComponents(container)
    }

    private val spawnTime = System.currentTimeMillis()
    private val animationDuration = 500L
    private val thisPageProgress = thisIndex.toFloat() / parent.pageSize

    init {
        addInnerWidget(widgetComponents.container)
        LogSystem.log("${children().size}")
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
        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.textStyle.size = 16f
        graphics2D.fillStyle = colorScheme.foregroundColor
        graphics2D.textCentered(category.name, width / 2f, graphics2D.textStyle.size)
        return graphics2D
    }

    override fun onSelected(data: LocalCategory) {
        println("Selected: ${data.translation()}")
    }
}
