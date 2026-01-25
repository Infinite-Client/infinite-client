package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.ui.layout.ScrollableLayoutContainer
import org.infinite.utils.alpha
import org.infinite.utils.mix
import kotlin.math.pow

abstract class ListCategoryWidget<T : Category<*, out Feature>>(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val data: T,
    title: Component,
) : AbstractWidget(x, y, width, height, title) {

    protected lateinit var container: ScrollableLayoutContainer
    private val spawnTime = System.currentTimeMillis()
    private val animationDuration = 500L
    private val containerMargin = 16
    private val headerHeight = 52
    private var searchQuery = ""
    private val scrollbarWidth = 20

    init {
        rebuildContent(width)
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

        // コンテナの再生成
        container = ScrollableLayoutContainer(innerLayout, innerWidth).apply {
            this.x = this@ListCategoryWidget.x + containerMargin
            this.setMinWidth(availableWidth)
        }
        updateLayout(newWidth, height)
    }

    private fun updateLayout(newWidth: Int, newHeight: Int) {
        if (!this::container.isInitialized) return

        val scrollY = this.y + headerHeight + containerMargin / 2
        val containerHeight = (newHeight - containerMargin - headerHeight).coerceAtLeast(10)

        container.x = this.x + containerMargin
        container.y = scrollY
        container.setMaxHeight(containerHeight)
        container.setMinWidth(newWidth - 2 * containerMargin)
    }

    // AbstractWidgetの必須実装
    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput)
    }

    // 座標やサイズが変更された際にレイアウトを追従させる
    override fun setX(x: Int) {
        super.setX(x)
        updateLayout(width, height)
    }
    override fun setY(y: Int) {
        super.setY(y)
        updateLayout(width, height)
    }
    override fun setWidth(width: Int) {
        super.setWidth(width)
        rebuildContent(width)
    }
    override fun setHeight(height: Int) {
        super.setHeight(height)
        updateLayout(width, height)
    }

    abstract fun buildContent(layout: LinearLayout, width: Int, query: String)

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val colorScheme = InfiniteClient.theme.colorScheme

        // アニメーション計算
        val progress = ((System.currentTimeMillis() - spawnTime).toFloat() / animationDuration).coerceIn(0f, 1f)
        val eased = (1f - (1f - progress).pow(3f)).coerceIn(0f, 1f)

        val w = width.toFloat()
        val h = height.toFloat()
        val radius = 14f

        // Graphics2DRenderer の初期化
        val renderer = Graphics2DRenderer(guiGraphics)

        renderer.push()
        // ウィジェットの基点 (x, y) へ移動
        renderer.translate(x.toFloat(), y.toFloat())

        // 背景色の計算
        val cardAlpha = (150 + (105 * eased)).toInt()
        val cardBaseColor = colorScheme.surfaceColor.mix(colorScheme.backgroundColor, 0.35f)

        // 1. メインカードの描画
        renderer.fillStyle = cardBaseColor.alpha(cardAlpha)
        renderer.fillRoundedRect(0f, 0f, w, h, radius)

        // 2. ヘッダー部分の描画
        renderer.fillStyle = cardBaseColor.mix(colorScheme.backgroundColor, 0.12f).alpha(cardAlpha)
        renderer.fillRoundedRect(0f, 0f, w, headerHeight.toFloat(), radius)

        // 3. テキストの描画
        val categoryName = data.name

        // タイトル (Bold)
        renderer.textStyle.font = "infinite_bold" // 必要に応じてフォント名を調整
        renderer.textStyle.size = 18f
        renderer.fillStyle = colorScheme.foregroundColor
        renderer.text(categoryName, 48f, 26f)

        // 説明文 (Regular)
        val description = categoryDescription()
        val subText =
            description.ifBlank { "${data.features.size} modules" }

        renderer.textStyle.font = "infinite_regular"
        renderer.textStyle.size = 11f
        renderer.fillStyle = colorScheme.secondaryColor
        renderer.text(subText, 48f, 40f)

        renderer.pop()

        // 描画コマンドの実行
        renderer.flush()

        // 内部コンテナ（スクロールエリア）の描画
        // 注意: container 内部でも Graphics2DRenderer を使っている場合は、そこでも flush() が必要です
        container.render(guiGraphics, mouseX, mouseY, delta)
    }

    private fun categoryDescription(): String {
        val key = data.translation()
        val translated = Component.translatable(key).string
        return if (translated != key && translated != data.name) translated else ""
    }

    // マウス入力の委譲
    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean = container.mouseClicked(mouseButtonEvent, bl)

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean = container.mouseScrolled(mouseX, mouseY, horizontal, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
}
