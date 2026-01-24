package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.ui.layout.ScrollableLayoutContainer
import org.infinite.libs.ui.screen.AbstractCarouselScreen
import org.infinite.libs.ui.widgets.AbstractCarouselWidget
import org.infinite.utils.alpha
import org.infinite.utils.mix
import kotlin.math.pow
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

    protected lateinit var container: ScrollableLayoutContainer
    private val spawnTime = System.currentTimeMillis()
    private val animationDuration = 500L
    private val thisPageProgress = thisIndex.toFloat() / parent.pageSize

    private val containerMargin = 16
    private val headerHeight = 52
    private var searchQuery = ""
    private val scrollbarWidth = 20

    init {
        val widgetWidth = parent.widgetWidth.roundToInt()
        rebuildContent(widgetWidth)

        // 初回のレイアウト更新を呼び出す
        updateLayout(width, height)
    }

    fun setSearchQuery(query: String) {
        val normalized = query.trim().lowercase()
        if (normalized == searchQuery) return
        searchQuery = normalized
        rebuildContent(width)
    }

    private fun rebuildContent(newWidth: Int) {
        val availableWidth = newWidth - 2 * containerMargin
        val innerWidth = (availableWidth - scrollbarWidth).coerceAtLeast(120)
        val innerLayout = LinearLayout.vertical().spacing(8)
        buildContent(innerLayout, innerWidth, searchQuery)
        innerLayout.arrangeElements()

        if (this::container.isInitialized) {
            children.remove(container)
        }

        container = ScrollableLayoutContainer(innerLayout, innerWidth).apply {
            this.x = containerMargin
            this.setMinWidth(availableWidth)
        }
        addInnerWidget(container)
        updateLayout(newWidth, height)
    }

    /**
     * GUIスケールや画面サイズが変更された際に、内部ウィジェットのサイズを再計算します。
     */
    private fun updateLayout(newWidth: Int, newHeight: Int) {
        val scrollY = headerHeight + containerMargin / 2
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

    abstract fun buildContent(layout: LinearLayout, width: Int, query: String)

    override fun render(graphics2D: AbstractCarouselScreen.WidgetGraphics2D): AbstractCarouselScreen.WidgetGraphics2D {
        val colorScheme = InfiniteClient.theme.colorScheme
        val progress = ((System.currentTimeMillis() - spawnTime).toFloat() / animationDuration).coerceIn(0f, 1f)
        val eased = (1f - (1f - progress).pow(3f)).coerceIn(0f, 1f)
        val w = graphics2D.width.toFloat()
        val h = graphics2D.height.toFloat()

        val radius = (w.coerceAtMost(h) * 0.045f).coerceAtLeast(14f)
        val cardAlpha = (150 + (105 * eased)).toInt().coerceIn(0, 255)
        val shadowAlpha = (40 + (50 * eased)).toInt().coerceIn(0, 255)
        val shift = (1f - eased) * 8f
        val scale = 0.96f + eased * 0.04f

        graphics2D.push()
        graphics2D.translate(w / 2f, h / 2f + shift)
        graphics2D.scale(scale, scale)
        graphics2D.translate(-w / 2f, -h / 2f)

        graphics2D.fillStyle = colorScheme.backgroundColor.alpha(shadowAlpha)
        graphics2D.fillRoundedRect(4f, 6f, w - 8f, h - 8f, radius + 2f)

        val cardColor = colorScheme.surfaceColor.mix(colorScheme.backgroundColor, 0.35f).alpha(cardAlpha)
        graphics2D.fillStyle = cardColor
        graphics2D.fillRoundedRect(0f, 0f, w, h, radius)

        val headerColor = cardColor.mix(colorScheme.backgroundColor, 0.12f)
        graphics2D.fillStyle = headerColor
        graphics2D.fillRoundedRect(0f, 0f, w, headerHeight.toFloat(), radius)

        val accent = colorScheme.color(360 * thisPageProgress, 0.7f, 0.5f, 1f)
        graphics2D.fillStyle = accent.alpha(220)
        graphics2D.fillRoundedRect(10f, 10f, 30f, 30f, 10f)

        val categoryName = data.name
        val iconText = if (categoryName.isNotEmpty()) categoryName.first().toString() else "?"
        graphics2D.textStyle.font = "infinite_bolditalic"
        graphics2D.textStyle.size = 16f
        graphics2D.fillStyle = colorScheme.foregroundColor
        graphics2D.textCentered(iconText, 25f, 30f)

        graphics2D.textStyle.font = "infinite_bolditalic"
        graphics2D.textStyle.size = 18f
        graphics2D.text(categoryName, 48f, 26f)

        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.textStyle.size = 11f
        graphics2D.fillStyle = colorScheme.secondaryColor
        val description = categoryDescription()
        val subText = if (description.isNotBlank()) description else "${data.features.size} modules"
        graphics2D.text(subText, 48f, 40f)

        graphics2D.pop()

        return graphics2D
    }

    private fun categoryDescription(): String {
        val key = data.translation()
        val translated = Component.translatable(key).string
        return if (translated != key && translated != data.name) translated else ""
    }
}
