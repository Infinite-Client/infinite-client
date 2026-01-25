package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
import org.infinite.utils.mix
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Theme システムに依存した ClickGui 抽象クラス
 */
abstract class ClickGuiScreen<T : Category<*, out Feature>>(
    title: Component,
    private val parent: Screen? = null,
) : Screen(title) {
    protected abstract val categories: List<T>

    // レイアウト定数
    private val padding = 12f
    private val sidebarWidth = 190f
    private val rowHeight = 22f
    private val rowGap = 4f
    private val headerHeight = 24f
    private val searchHeight = 18f
    private val searchGap = 10f
    private val rowRadius = 6f
    private val panelRadius = 10f
    private val settingsWidth = 44f
    private val resetBtnWidth = 32f
    private val uiScale = 1.0f

    private var selectedCategory: T? = null
    protected var searchQuery: String = ""
    private var featureEntries: List<Pair<T, Feature>> = emptyList()

    private var categoryScroll = 0.0
    private var categoryScrollTarget = 0.0
    private var featureScroll = 0.0
    private var featureScrollTarget = 0.0

    private lateinit var searchBox: EditBox

    private val categoryHover = mutableListOf<Float>()
    private val featureHover = mutableListOf<Float>()
    private val featureEnable = LinkedHashMap<Feature, Float>()
    private var searchFocus = 0f
    private var openProgress = 1f
    private var openEase = 1f
    private var openTime = System.currentTimeMillis()

    private val themeScheme get() = InfiniteClient.theme.colorScheme

    override fun init() {
        clearWidgets()
        openTime = System.currentTimeMillis()
        buildSearchBox()
        rebuildFeatureList()
    }

    protected open fun openFeatureSettings(feature: Feature) {
        minecraft.setScreen(ListFeatureScreen(feature, this))
    }

    protected fun resetFeature(feature: Feature) {
        feature.reset()
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    private fun buildSearchBox() {
        searchBox = EditBox(
            minecraft.font,
            padding.toInt(),
            padding.toInt(),
            (width - padding.toInt() * 2).coerceAtLeast(140),
            searchHeight.toInt(),
            Component.literal("Search"),
        ).apply {
            setMaxLength(60)
            isBordered = false
            setTextColor(themeScheme.foregroundColor)
            value = searchQuery
            setResponder {
                searchQuery = it
                rebuildFeatureList()
            }
        }
        addRenderableWidget(searchBox)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val g2d = Graphics2DRenderer(guiGraphics)

        // アニメーション計算
        openProgress = ((System.currentTimeMillis() - openTime) / 220f).coerceIn(0f, 1f)
        openEase = openProgress * openProgress * (3f - 2f * openProgress)
        val pulse = ((sin(System.currentTimeMillis() / 240.0) + 1.0) * 0.5).toFloat()

        // UI座標変換 (Graphics2DのTransformを使用)
        g2d.push()
        g2d.translate(uiOffsetX(), uiOffsetY())
        g2d.scale(uiScale, uiScale)

        val uiMouseX = toUiX(mouseX.toDouble()).toInt()
        val uiMouseY = toUiY(mouseY.toDouble()).toInt()

        // 背景描画
        InfiniteClient.theme.renderBackGround(0f, 0f, width.toFloat(), height.toFloat(), g2d, openEase * 0.1f)

        val panelTop = padding + searchHeight + searchGap
        val contentX = padding * 2 + sidebarWidth
        val contentW = width - contentX - padding
        val panelH = height - panelTop - padding

        // --- メインパネル (Sidebar & Content) ---
        g2d.fillStyle = themeScheme.surfaceColor.alpha((255 * openEase).toInt())
        g2d.fillRoundedRect(padding, panelTop, sidebarWidth, panelH, panelRadius)
        g2d.fillRoundedRect(contentX, panelTop, contentW, panelH, panelRadius)

        // ヘッダー
        g2d.fillStyle = themeScheme.getHoverColor(themeScheme.surfaceColor).alpha((255 * openEase).toInt())
        g2d.fillRoundedRect(padding, panelTop, sidebarWidth, headerHeight, panelRadius)
        g2d.fillRoundedRect(contentX, panelTop, contentW, headerHeight, panelRadius)

        // テキスト描画 (Graphics2D.textを使用)
        g2d.textStyle.font = "infinite_bolditalic"
        g2d.textStyle.shadow = true
        val fontSize = 12f
        g2d.textStyle.size = fontSize
        val textHeight = 9f // Minecraft FontRendererの標準
        val textYOffset = (headerHeight - textHeight) / 2f
        g2d.fillStyle = themeScheme.foregroundColor.alpha((255 * openEase).toInt())
        g2d.text(
            "Categories",
            padding + 8,
            panelTop + textYOffset,
        )
        g2d.text(
            title.string,
            contentX + 8,
            panelTop + textYOffset,
        )
        // スクロール更新
        updateScrollTargets()
        categoryScroll += (categoryScrollTarget - categoryScroll) * 0.35
        featureScroll += (featureScrollTarget - featureScroll) * 0.35

        renderSearchBox(g2d)

        // --- カテゴリリスト (Scissor適用) ---
        val catListY = panelTop + headerHeight + padding / 2
        val catListH = panelH - headerHeight - padding
        g2d.enableScissor(padding.toInt(), catListY.toInt(), sidebarWidth.toInt(), catListH.toInt())
        renderCategoryRows(g2d, uiMouseX, uiMouseY, catListY, catListH)
        g2d.disableScissor()

        // --- フィーチャーリスト (Scissor適用) ---
        val featListY = panelTop + headerHeight
        val featListH = panelH - headerHeight - padding
        g2d.enableScissor(contentX.toInt(), featListY.toInt(), contentW.toInt(), featListH.toInt())
        renderFeatureRows(
            g2d,
            uiMouseX,
            uiMouseY,
            pulse,
            contentX + padding,
            featListY,
            contentW - padding * 2,
            featListH,
        )
        g2d.disableScissor()

        g2d.pop()
        g2d.flush() // 最後にまとめてレンダリング

        // Widget (EditBox) の描画は最後に
        super.render(guiGraphics, uiMouseX, uiMouseY, delta)
    }

    private fun renderCategoryRows(g2d: Graphics2DRenderer, mx: Int, my: Int, y: Float, h: Float) {
        val x = 24f
        val width = sidebarWidth - padding * 2
        ensureSize(categoryHover, categories.size)

        categories.forEachIndexed { index, category ->
            val rowTop = y + index * (rowHeight + rowGap) - categoryScroll.toFloat()
            if (rowTop + rowHeight < y || rowTop > y + h) return@forEachIndexed

            val reveal = ((openProgress * 1.4f) - index * 0.05f).coerceIn(0f, 1f)
            val selected = category == selectedCategory
            val hovered = mx in x.toInt()..(x + width).toInt() && my in rowTop.toInt()..(rowTop + rowHeight).toInt()
            val hover = updateHover(categoryHover, index, if (hovered) 1f else 0f)

            val baseColor = if (selected) themeScheme.secondaryColor.alpha(180) else themeScheme.surfaceColor
            g2d.fillStyle =
                (if (hover > 0.01f) themeScheme.getHoverColor(baseColor) else baseColor).alpha((255 * openEase * reveal).toInt())

            g2d.fillRoundedRect(x, rowTop, width, rowHeight, rowRadius)

            if (selected) {
                g2d.fillStyle = themeScheme.accentColor.alpha((255 * openEase * reveal).toInt())
                g2d.fillRoundedRect(x, rowTop, 3f, rowHeight, 2f)
            }

            g2d.fillStyle = themeScheme.foregroundColor.alpha((255 * openEase * reveal).toInt())
            g2d.textStyle.font = "infinite_regular"
            g2d.textStyle.shadow = true
            g2d.textStyle.size = 9f
            g2d.text(category.name, x + 8, rowTop + (rowHeight - 8) / 2f)
        }
    }

    private fun renderFeatureRows(
        g2d: Graphics2DRenderer,
        mx: Int,
        my: Int,
        pulse: Float,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
    ) {
        ensureSize(featureHover, featureEntries.size)
        featureEntries.forEachIndexed { index, (category, feature) ->
            val rowTop = y + index * (rowHeight + rowGap) - featureScroll.toFloat()
            if (rowTop + rowHeight < y || rowTop > y + h) return@forEachIndexed

            val reveal = ((openProgress * 1.3f) - index * 0.035f).coerceIn(0f, 1f)
            val alphaFactor = openEase * reveal

            val enabledVal = updateFeatureEnable(feature, if (feature.isEnabled()) 1f else 0f)
            val hovered = mx in x.toInt()..(x + w).toInt() && my in rowTop.toInt()..(rowTop + rowHeight).toInt()
            val hover = updateHover(featureHover, index, if (hovered) 1f else 0f)

            // 背景：基本はSurface。ホバー時は少し明るく。
            val baseColor = themeScheme.surfaceColor
            val color = if (hover > 0.01f) themeScheme.getHoverColor(baseColor) else baseColor
            g2d.fillStyle = color.alpha(alphaFactor)
            g2d.fillRoundedRect(x, rowTop, w, rowHeight, rowRadius)

            // --- Pulse を活かしたインジケーター ---
            if (enabledVal > 0.01f) {
                // pulse を使って 0.6f 〜 1.0f の間でアルファを揺らす
                val pulseAlpha = (0.6f + 0.4f * pulse) * enabledVal
                g2d.fillStyle = themeScheme.accentColor.alpha(alphaFactor * pulseAlpha)
                g2d.fillRoundedRect(x, rowTop, 2.5f, rowHeight, 1.5f)

                // 外側に薄い光（Glow）を追加して鼓動感を強調
                g2d.fillStyle = themeScheme.accentColor.alpha(alphaFactor * 0.2f * pulse * enabledVal)
                g2d.fillRoundedRect(x, rowTop, 5f, rowHeight, 2f)
            }

            // コントロール描画 (pulse を渡すように変更)
            renderFeatureControls(g2d, x, rowTop, w, enabledVal, alphaFactor, pulse, mx, my)

            // --- Pulse を活かしたテキスト ---
            // 有効時はテキスト自体も pulse に合わせて微妙に色が変わるように
            val textColor = if (enabledVal > 0.5f) {
                // accentColor と foregroundColor を pulse でブレンド（あるいはアルファ揺らし）
                val mixColor = themeScheme.accentColor
                themeScheme.foregroundColor.mix(mixColor, 0.2f).alpha(alphaFactor * (0.8f + 0.2f * pulse))
            } else {
                themeScheme.foregroundColor.alpha(alphaFactor)
            }

            val displayName = if (searchQuery.isBlank()) feature.name else "${feature.name} [${category.name}]"
            g2d.fillStyle = textColor
            g2d.textStyle.font = "infinite_regular"
            g2d.textStyle.shadow = true
            g2d.textStyle.size = 9f
            g2d.text(displayName, x + 8, rowTop + (rowHeight - 8) / 2f)
        }
    }

    private fun renderFeatureControls(
        g2d: Graphics2DRenderer,
        x: Float,
        y: Float,
        w: Float,
        enabled: Float,
        alphaFactor: Float,
        pulse: Float,
        mx: Int,
        my: Int,
    ) {
        val ctrlH = rowHeight - 8f
        val setX = x + w - settingsWidth + 5f
        val resetX = setX - resetBtnWidth - 4f
        val toggleW = 32f
        val toggleX = resetX - toggleW - 8f

        // --- トグルスイッチ ---
        // 背景
        g2d.fillStyle = themeScheme.backgroundColor.alpha(alphaFactor * 0.4f)
        g2d.fillRoundedRect(toggleX, y + 4f, toggleW, ctrlH, ctrlH / 2f)

        // 有効時のみノブの周りに pulse 光彩を追加
        if (enabled > 0.1f) {
            g2d.fillStyle = themeScheme.accentColor.alpha(alphaFactor * 0.3f * pulse * enabled)
            g2d.fillRoundedRect(toggleX - 1f, y + 3f, toggleW + 2f, ctrlH + 2f, (ctrlH + 2f) / 2f)
        }

        // ノブ
        val knobColor = if (enabled > 0.5f) themeScheme.accentColor else themeScheme.secondaryColor
        g2d.fillStyle = knobColor.alpha(alphaFactor)
        val knobPadding = 2f
        val knobSize = ctrlH - (knobPadding * 2)
        val knobX = toggleX + knobPadding + (toggleW - knobSize - (knobPadding * 2)) * enabled
        g2d.fillRoundedRect(knobX, y + 4f + knobPadding, knobSize, knobSize, knobSize / 2f)

        // ボタン描画
        drawButton(g2d, "R", resetX, y + 4f, resetBtnWidth, ctrlH, mx, my, alphaFactor)
        drawButton(g2d, "SET", setX, y + 4f, settingsWidth - 10f, ctrlH, mx, my, alphaFactor)
    }

    private fun drawButton(
        g2d: Graphics2DRenderer,
        label: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        mx: Int,
        my: Int,
        alpha: Float,
    ) {
        val hovered = mx in x.toInt()..(x + w).toInt() && my in y.toInt()..(y + h + 4).toInt() // rowHeight分判定を広げる
        g2d.fillStyle = (if (hovered) themeScheme.accentColor.alpha(100) else themeScheme.surfaceColor).alpha(alpha)
        g2d.fillRoundedRect(x, y, w, h, h / 2f)

        g2d.fillStyle = themeScheme.secondaryColor.alpha(alpha)
        g2d.textCentered(label, x + w / 2f, y + (h - 8f) / 2f + 4f)
    }

    // --- その他ヘルパー ---
    private fun renderSearchBox(g2d: Graphics2DRenderer) {
        val focusTarget = if (searchBox.isFocused) 1f else 0f
        searchFocus += (focusTarget - searchFocus) * 0.25f
        val alpha = (255 * openEase).toInt()

        g2d.fillStyle = themeScheme.surfaceColor.alpha(alpha)
        g2d.fillRoundedRect(
            searchBox.x.toFloat(),
            searchBox.y.toFloat(),
            searchBox.width.toFloat(),
            searchBox.height.toFloat(),
            9f,
        )

        if (searchFocus > 0.01f) {
            g2d.strokeStyle.color = themeScheme.accentColor.alpha((100 * searchFocus * openEase).toInt())
            g2d.strokeRoundedRect(
                searchBox.x - 1.5f,
                searchBox.y - 1.5f,
                searchBox.width + 3f,
                searchBox.height + 3f,
                10f,
            )
        }
    }

    // --- 以降、スクロール計算やマウス判定ロジック (変更なし) ---
    private fun rebuildFeatureList() {
        val query = searchQuery.trim()
        featureEntries = if (query.isBlank()) {
            selectedCategory?.features?.values?.map { selectedCategory!! to it } ?: emptyList()
        } else {
            categories.flatMap { cat -> cat.features.values.map { cat to it } }
                .filter { it.second.name.contains(query, ignoreCase = true) }
        }
    }

    private fun updateHover(state: MutableList<Float>, index: Int, target: Float): Float {
        ensureSize(state, index + 1)
        state[index] += (target - state[index]) * 0.2f
        return state[index]
    }

    private fun updateFeatureEnable(feature: Feature, target: Float): Float {
        featureEnable[feature] =
            (featureEnable[feature] ?: target) + (target - (featureEnable[feature] ?: target)) * 0.18f
        return featureEnable[feature]!!
    }

    private fun ensureSize(state: MutableList<Float>, size: Int) {
        while (state.size < size) state.add(0f)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = toUiX(mouseButtonEvent.x).toInt()
        val my = toUiY(mouseButtonEvent.y).toInt()

        if (searchBox.isMouseOver(mx.toDouble(), my.toDouble())) {
            searchBox.isFocused = true
            return searchBox.mouseClicked(mouseButtonEvent, bl)
        }
        searchBox.isFocused = false

        if (mx.toFloat() in padding..(padding + sidebarWidth)) {
            val index = rowIndexFromMouse(
                my.toDouble(),
                (padding + searchHeight + searchGap + headerHeight + padding / 2).roundToInt(),
                height,
                categoryScroll,
            )
            if (index in categories.indices) {
                selectCategory(categories[index])
                return true
            }
        }

        val contentX = padding * 2 + sidebarWidth
        val contentW = width - contentX - padding
        if (mx.toFloat() in contentX..(width - padding)) {
            val index = rowIndexFromMouse(
                my.toDouble(),
                (padding + searchHeight + searchGap + headerHeight).roundToInt(),
                height,
                featureScroll,
            )
            if (index in featureEntries.indices) {
                val (_, feature) = featureEntries[index]
                val setX = contentX + padding + contentW - padding * 2 - settingsWidth + 5f
                val resetX = setX - resetBtnWidth - 4f
                val toggleX = resetX - 36f - 8f

                when {
                    mx > setX -> openFeatureSettings(feature)

                    mx > resetX -> {
                        minecraft.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f))
                        resetFeature(feature)
                    }

                    mx > toggleX -> {
                        if (feature.isEnabled()) feature.disable() else feature.enable()
                    }
                }
                return true
            }
        }
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    private fun updateScrollTargets() {
        val listH = height - (padding + searchHeight + searchGap) - padding - headerHeight - padding
        categoryScrollTarget = clamp(categoryScrollTarget, max(0.0, totalRowsHeight(categories.size) - listH))
        featureScrollTarget = clamp(featureScrollTarget, max(0.0, totalRowsHeight(featureEntries.size) - listH))
    }

    private fun selectCategory(category: T) {
        if (selectedCategory == category) return
        selectedCategory = category
        if (searchQuery.isBlank()) rebuildFeatureList()
    }

    private fun totalRowsHeight(count: Int): Double = if (count <= 0) 0.0 else count * (rowHeight + rowGap) - rowGap.toDouble()

    private fun clamp(value: Double, max: Double): Double = value.coerceIn(0.0, max)
    private fun rowIndexFromMouse(mouseY: Double, listY: Int, listH: Int, scroll: Double): Int = if (mouseY < listY || mouseY > listY + listH) -1 else ((mouseY - listY + scroll) / (rowHeight + rowGap)).toInt()

    private fun uiOffsetX(): Float = (width - width * uiScale) / 2f
    private fun uiOffsetY(): Float = (height - height * uiScale) / 2f
    private fun toUiX(x: Double): Double = (x - uiOffsetX()) / uiScale
    private fun toUiY(y: Double): Double = (y - uiOffsetY()) / uiScale
}
