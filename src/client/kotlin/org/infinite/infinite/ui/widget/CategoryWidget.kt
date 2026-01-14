package org.infinite.infinite.ui.widget

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

    // 定数を定義
    private val font = Font("infinite_regular")
    private val containerMargin = 10

    init {
        val widgetWidth = parent.widgetWidth.roundToInt()
        val innerWidth = widgetWidth - 2 * containerMargin

        // 内部レイアウトの構築
        val innerLayout = LinearLayout.vertical().spacing(5)
        buildContent(innerLayout, innerWidth)
        innerLayout.arrangeElements()

        // コンテナの初期化（サイズは後で更新するためここでは仮置き）
        container = ScrollableLayoutContainer(innerLayout, innerWidth).apply {
            this.x = containerMargin
            this.setMinWidth(innerWidth)
        }

        addInnerWidget(container)

        // 初回のレイアウト更新を呼び出す
        updateLayout(width, height)
    }

    /**
     * GUIスケールや画面サイズが変更された際に、内部ウィジェットのサイズを再計算します。
     */
    private fun updateLayout(newWidth: Int, newHeight: Int) {
        val titleY = font.lineHeight * 2
        val scrollY = titleY + font.lineHeight + containerMargin
        val containerHeight = (newHeight - containerMargin - scrollY).coerceAtLeast(10)

        container.y = scrollY
        container.setMaxHeight(containerHeight)
        container.setMinWidth(newWidth - 2 * containerMargin)
    }

    /**
     * Minecraftのウィジェットのリサイズイベントをフックします（バージョンによりメソッド名が異なる場合があります）
     */
    override fun setHeight(value: Int) {
        super.setHeight(value)
        updateLayout(width, value)
    }

    override fun setWidth(value: Int) {
        super.setWidth(value)
        updateLayout(value, height)
    }

    override fun setX(i: Int) {
        super.setX(i)
        updateLayout(width, height)
    }

    override fun setY(i: Int) {
        super.setY(i)
        updateLayout(width, height)
    }

    override fun setPosition(i: Int, j: Int) {
        super.setPosition(i, j)
        updateLayout(width, height)
    }

    override fun setSize(i: Int, j: Int) {
        super.setSize(i, j)
        updateLayout(i, j)
    }

    override fun setRectangle(i: Int, j: Int, k: Int, l: Int) {
        super.setRectangle(i, j, k, l)
        updateLayout(k, l)
    }

    abstract fun buildContent(layout: LinearLayout, width: Int)

    override fun render(graphics2D: AbstractCarouselScreen.WidgetGraphics2D): AbstractCarouselScreen.WidgetGraphics2D {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme

        val alpha = ((System.currentTimeMillis() - spawnTime).toFloat() / animationDuration * 0.5f).coerceIn(0f, 0.5f)
        val w = graphics2D.width.toFloat()
        val h = graphics2D.height.toFloat()

        theme.renderBackGround(0f, 0f, w, h, graphics2D, alpha)

        graphics2D.strokeStyle.width = 2f
        val startColor = colorScheme.color(360 * thisPageProgress, 1f, 0.5f, alpha)
        val endColor = colorScheme.color(360 * (thisPageProgress + 0.5f / parent.pageSize), 1f, 0.5f, alpha)
        graphics2D.strokeRect(0f, 0f, w, h, startColor, startColor, endColor, endColor)

        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.textStyle.size = 16f
        graphics2D.fillStyle = colorScheme.foregroundColor
        graphics2D.textCentered(data.name, w / 2f, graphics2D.textStyle.size)

        return graphics2D
    }
}
