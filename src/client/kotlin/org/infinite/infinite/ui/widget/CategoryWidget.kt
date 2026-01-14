package org.infinite.infinite.ui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.ui.layout.ScrollableLayoutContainer
import org.infinite.libs.ui.screen.AbstractCarouselScreen
import org.infinite.libs.ui.widgets.AbstractCarouselWidget
import org.infinite.utils.Font
import kotlin.math.roundToInt

abstract class CategoryWidget<T : Category<*, out Feature>>(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    data: T,
    parent: AbstractCarouselScreen<T>,
    index: Int,
    title: Component,
) : AbstractCarouselWidget<T>(x, y, width, height, data, parent, index, title) {

    protected val container: ScrollableLayoutContainer
    private val spawnTime = System.currentTimeMillis()
    private val animationDuration = 500L
    private val thisPageProgress = thisIndex.toFloat() / parent.pageSize
    private val minecraft = Minecraft.getInstance()

    init {
        val font = Font("infinite_regular")

        val widgetWidth = parent.widgetWidth.roundToInt()
        val titleY = font.lineHeight * 2
        val containerMargin = 10
        val innerWidth = widgetWidth - 2 * containerMargin
        val scrollY = titleY + font.lineHeight + containerMargin
        val containerHeight = height - containerMargin - titleY

        // 内部レイアウトの構築
        val innerLayout = LinearLayout.vertical().spacing(5)
        buildContent(innerLayout, innerWidth)

        // レイアウトの確定
        innerLayout.arrangeElements()

        container = ScrollableLayoutContainer(innerLayout, innerWidth).apply {
            this.x = containerMargin
            this.y = scrollY
            // maxHeightだけでなく、ウィジェット自体の高さを固定または最大まで広げる
            this.setMaxHeight(containerHeight)
            this.setMinWidth(innerWidth)
        }

        addInnerWidget(container)
    }

    /**
     * 各カテゴリに応じたコンテンツ（LocalFeatureWidget 等）をレイアウトに追加します。
     */
    abstract fun buildContent(layout: LinearLayout, width: Int)

    override fun render(graphics2D: AbstractCarouselScreen.WidgetGraphics2D): AbstractCarouselScreen.WidgetGraphics2D {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme

        // フェードインアニメーション
        val alpha = ((System.currentTimeMillis() - spawnTime).toFloat() / animationDuration * 0.5f).coerceIn(0f, 0.5f)
        val w = graphics2D.width.toFloat()
        val h = graphics2D.height.toFloat()

        theme.renderBackGround(0f, 0f, w, h, graphics2D, alpha)

        // カテゴリの色（虹色）の枠線
        graphics2D.strokeStyle.width = 2f
        val startColor = colorScheme.color(360 * thisPageProgress, 1f, 0.5f, alpha)
        val endColor = colorScheme.color(360 * (thisPageProgress + 0.5f / parent.pageSize), 1f, 0.5f, alpha)
        graphics2D.strokeRect(0f, 0f, w, h, startColor, startColor, endColor, endColor)

        // タイトル描画（data.name を直接利用可能）
        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.textStyle.size = 16f
        graphics2D.fillStyle = colorScheme.foregroundColor
        graphics2D.textCentered(data.name, w / 2f, graphics2D.textStyle.size)

        return graphics2D
    }
}
