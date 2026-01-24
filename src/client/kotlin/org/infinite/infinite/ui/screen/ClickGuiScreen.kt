package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.ClickGuiPalette
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
import org.infinite.utils.mix
import kotlin.math.max
import kotlin.math.sin

/**
 * T: 描画対象のデータ型 (Category)
 */
abstract class ClickGuiScreen<T : Category<*, out Feature>>(
    title: Component,
    private val parent: Screen? = null,
) : Screen(title) {

    protected abstract val categories: List<T>

    private val padding = 12
    private val sidebarWidth = 190
    private val rowHeight = 22
    private val rowGap = 4
    private val headerHeight = 24
    private val searchHeight = 18
    private val searchGap = 10
    private val rowRadius = 6f
    private val panelRadius = 10f
    private val settingsWidth = 44
    private val uiScale = 1.0f

    private var selectedCategory: T? = null
    protected var searchQuery: String = "" // サブクラスからアクセス可能に
    private var featureEntries: List<Pair<T, Feature>> = emptyList()

    private var categoryScroll = 0.0
    private var categoryScrollTarget = 0.0
    private var featureScroll = 0.0
    private var featureScrollTarget = 0.0

    private lateinit var searchBox: EditBox
    private data class TextDraw(val text: String, val x: Int, val y: Int, val color: Int)
    private val categoryHover = mutableListOf<Float>()
    private val featureHover = mutableListOf<Float>()
    private val featureEnable = LinkedHashMap<Feature, Float>()
    private var searchFocus = 0f
    private var openProgress = 1f
    private var openEase = 1f
    private var openTime = System.currentTimeMillis()

    /**
     * 設定画面を開くための抽象メソッド。UiStyleRegistryの代わりに各実装クラスで定義します。
     */
    protected abstract fun openFeatureSettings(feature: Feature)

    override fun init() {
        clearWidgets()
        openTime = System.currentTimeMillis()
        searchFocus = 0f
        categoryHover.clear()
        featureHover.clear()
        featureEnable.clear()

        val available = categories
        if (available.isNotEmpty() && (selectedCategory == null || selectedCategory !in available)) {
            selectedCategory = available.first()
        }

        buildSearchBox()
        rebuildFeatureList(true)
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {}

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val uiMouseX = toUiX(mouseX.toDouble()).toInt()
        val uiMouseY = toUiY(mouseY.toDouble()).toInt()
        val panelTop = padding + searchHeight + searchGap
        val sidebarX = padding
        val sidebarH = height - panelTop - padding
        val contentX = padding * 2 + sidebarWidth
        val contentW = width - contentX - padding
        val contentH = height - panelTop - padding

        val pulse = ((sin(System.currentTimeMillis() / 240.0) + 1.0) * 0.5).toFloat()

        openProgress = ((System.currentTimeMillis() - openTime) / 220f).coerceIn(0f, 1f)
        openEase = openProgress * openProgress * (3f - 2f * openProgress)

        val backdrop = Graphics2DRenderer(guiGraphics)
        backdrop.fillStyle = scaleAlpha(ClickGuiPalette.BACKDROP, openEase)
        backdrop.fillRect(0f, 0f, width.toFloat(), height.toFloat())
        backdrop.flush()

        guiGraphics.pose().pushMatrix()
        guiGraphics.pose().translate(uiOffsetX(), uiOffsetY())
        guiGraphics.pose().scale(uiScale, uiScale)

        val g2d = Graphics2DRenderer(guiGraphics)
        val textDraws = mutableListOf<TextDraw>()

        // パネル描画
        g2d.fillStyle = scaleAlpha(ClickGuiPalette.PANEL, openEase)
        g2d.fillRoundedRect(
            sidebarX.toFloat(),
            panelTop.toFloat(),
            sidebarWidth.toFloat(),
            sidebarH.toFloat(),
            panelRadius,
        )
        g2d.fillRoundedRect(contentX.toFloat(), panelTop.toFloat(), contentW.toFloat(), contentH.toFloat(), panelRadius)

        // ヘッダー背景
        g2d.fillStyle = scaleAlpha(ClickGuiPalette.PANEL_ALT, openEase)
        g2d.fillRoundedRect(
            sidebarX.toFloat(),
            panelTop.toFloat(),
            sidebarWidth.toFloat(),
            headerHeight.toFloat(),
            panelRadius,
        )
        g2d.fillRoundedRect(
            contentX.toFloat(),
            panelTop.toFloat(),
            contentW.toFloat(),
            headerHeight.toFloat(),
            panelRadius,
        )

        updateScrollTargets()
        categoryScroll += (categoryScrollTarget - categoryScroll) * 0.35
        featureScroll += (featureScrollTarget - featureScroll) * 0.35

        // 検索ボックス背景
        renderSearchBoxBackground(g2d)

        val font = minecraft.font
        val headerColor = scaleAlpha(ClickGuiPalette.MUTED, openEase)
        textDraws.add(TextDraw("Categories", sidebarX + 8, panelTop + 7, headerColor))
        textDraws.add(TextDraw(title.string, contentX + 8, panelTop + 7, headerColor))

        // カテゴリ・機能リスト描画
        renderCategoryRows(
            g2d, font, textDraws, uiMouseX, uiMouseY, sidebarX + padding,
            panelTop + headerHeight + padding / 2, sidebarWidth - padding * 2, sidebarH - headerHeight - padding,
        )
        renderFeatureRows(
            g2d, font, textDraws, uiMouseX, uiMouseY, pulse, contentX + padding,
            panelTop + headerHeight, contentW - padding * 2, contentH - headerHeight - padding,
        )

        g2d.flush()
        textDraws.forEach { guiGraphics.drawString(font, it.text, it.x, it.y, it.color, false) }

        super.render(guiGraphics, uiMouseX, uiMouseY, delta)
        guiGraphics.pose().popMatrix()
    }

    private fun renderSearchBoxBackground(g2d: Graphics2DRenderer) {
        val searchX = searchBox.x.toFloat()
        val searchY = searchBox.y.toFloat()
        val searchW = searchBox.width.toFloat()
        val searchH = searchBox.height.toFloat()
        val searchRadius = (searchH * 0.5f).coerceAtLeast(6f)
        val focusTarget = if (searchBox.isFocused) 1f else 0f
        searchFocus += (focusTarget - searchFocus) * 0.25f

        val searchFill = ClickGuiPalette.PANEL_ALT.mix(ClickGuiPalette.ACCENT_DARK, 0.2f * searchFocus)
        g2d.fillStyle = scaleAlpha(searchFill, openEase)
        g2d.fillRoundedRect(searchX, searchY, searchW, searchH, searchRadius)

        if (searchFocus > 0.01f) {
            g2d.fillStyle = ClickGuiPalette.ACCENT.alpha((70 * searchFocus * openEase).toInt())
            g2d.fillRoundedRect(searchX - 2f, searchY - 2f, searchW + 4f, searchH + 4f, searchRadius + 2f)
        }
    }

    private fun buildSearchBox() {
        searchBox = EditBox(minecraft.font, padding, padding, (width - padding * 2).coerceAtLeast(140), searchHeight, Component.literal("Search")).apply {
            setMaxLength(60)
            isBordered = false
            setTextColor(ClickGuiPalette.TEXT)
            value = searchQuery
            setResponder {
                searchQuery = it
                rebuildFeatureList(true)
            }
        }
        addRenderableWidget(searchBox)
    }

    private fun selectCategory(category: T) {
        if (selectedCategory == category) return
        selectedCategory = category
        if (searchQuery.isBlank()) rebuildFeatureList(true)
    }

    private fun rebuildFeatureList(resetScroll: Boolean) {
        val query = searchQuery.trim()
        featureEntries = if (query.isBlank()) {
            selectedCategory?.features?.values?.map { selectedCategory!! to it } ?: emptyList()
        } else {
            categories.flatMap { cat -> cat.features.values.map { cat to it } }
                .filter { it.second.name.contains(query, ignoreCase = true) }
        }
        if (resetScroll) {
            featureScroll = 0.0
            featureScrollTarget = 0.0
        }
    }

    private fun renderCategoryRows(g2d: Graphics2DRenderer, font: net.minecraft.client.gui.Font, textDraws: MutableList<TextDraw>, mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int) {
        ensureSize(categoryHover, categories.size)
        val stride = rowHeight + rowGap
        categories.forEachIndexed { index, category ->
            val rowTop = y + index * stride - categoryScroll
            if (rowTop + rowHeight < y || rowTop > y + height) return@forEachIndexed

            val reveal = ((openProgress * 1.4f) - index * 0.05f).coerceIn(0f, 1f)
            val selected = category == selectedCategory
            val hovered = mouseX in x..(x + width) && mouseY in rowTop.toInt()..(rowTop + rowHeight).toInt()
            val hover = updateHover(categoryHover, index, if (hovered) 1f else 0f, 0.2f)

            g2d.fillStyle = scaleAlpha(if (selected) ClickGuiPalette.ACCENT_DARK else ClickGuiPalette.PANEL_ALT, openEase * reveal).mix(ClickGuiPalette.HOVER, 0.25f * hover)
            g2d.fillRoundedRect(x.toFloat(), rowTop.toFloat(), width.toFloat(), rowHeight.toFloat(), rowRadius)

            if (selected) {
                g2d.fillStyle = scaleAlpha(ClickGuiPalette.ACCENT, openEase * reveal)
                g2d.fillRoundedRect(x.toFloat(), rowTop.toFloat(), 3f, rowHeight.toFloat(), 2f)
            }
            textDraws.add(TextDraw(category.name, x + 8, (rowTop + (rowHeight - 8) / 2).toInt(), scaleAlpha(ClickGuiPalette.TEXT, openEase * reveal)))
        }
    }

    private fun renderFeatureRows(g2d: Graphics2DRenderer, font: net.minecraft.client.gui.Font, textDraws: MutableList<TextDraw>, mouseX: Int, mouseY: Int, pulse: Float, x: Int, y: Int, width: Int, height: Int) {
        ensureSize(featureHover, featureEntries.size)
        val stride = rowHeight + rowGap
        featureEntries.forEachIndexed { index, (category, feature) ->
            val rowTop = y + index * stride - featureScroll
            if (rowTop + rowHeight < y || rowTop > y + height) return@forEachIndexed

            val reveal = ((openProgress * 1.3f) - index * 0.035f).coerceIn(0f, 1f)
            val enabledTarget = if (feature.isEnabled()) 1f else 0f
            val enabledValue = updateFeatureEnable(feature, enabledTarget)
            val hovered = mouseX in x..(x + width) && mouseY in rowTop.toInt()..(rowTop + rowHeight).toInt()
            val hover = updateHover(featureHover, index, if (hovered) 1f else 0f, 0.2f)

            val baseFill = ClickGuiPalette.PANEL_ALT.mix(ClickGuiPalette.ACCENT_DARK.mix(ClickGuiPalette.ACCENT, 0.4f), 0.22f * enabledValue + 0.08f * pulse * enabledValue)
            g2d.fillStyle = scaleAlpha(baseFill.mix(ClickGuiPalette.HOVER, 0.2f * hover), openEase * reveal)
            g2d.fillRoundedRect(x.toFloat(), rowTop.toFloat(), width.toFloat(), rowHeight.toFloat(), rowRadius)

            // トグル・設定ボタン描画 (簡易化)
            renderFeatureControls(g2d, font, textDraws, x, rowTop.toInt(), width, enabledValue, reveal, mouseX, mouseY)

            val name = if (searchQuery.isBlank()) feature.name else "${feature.name} [${category.name}]"
            textDraws.add(TextDraw(name, x + 8, (rowTop + (rowHeight - 8) / 2).toInt(), scaleAlpha(ClickGuiPalette.TEXT, openEase * reveal)))
        }
    }

    private fun renderFeatureControls(g2d: Graphics2DRenderer, font: net.minecraft.client.gui.Font, textDraws: MutableList<TextDraw>, x: Int, y: Int, w: Int, enabled: Float, reveal: Float, mx: Int, my: Int) {
        val ctrlH = rowHeight - 8f
        val setX = x + w - settingsWidth + 5f
        val toggleW = 36f
        val toggleX = setX - toggleW - 8f

        // トグル
        g2d.fillStyle = scaleAlpha(ClickGuiPalette.PANEL_ALT.mix(ClickGuiPalette.ACCENT_DARK, 0.25f * enabled), openEase * reveal)
        g2d.fillRoundedRect(toggleX, y + 4f, toggleW, ctrlH, ctrlH / 2f)
        g2d.fillStyle = scaleAlpha(ClickGuiPalette.TEXT.mix(ClickGuiPalette.MUTED, 1f - enabled), openEase * reveal)
        g2d.fillRoundedRect(toggleX + 2f + (toggleW - ctrlH) * enabled, y + 6f, ctrlH - 4f, ctrlH - 4f, (ctrlH - 4f) / 2f)

        // SETボタン
        g2d.fillStyle = scaleAlpha(ClickGuiPalette.PANEL_ALT.mix(ClickGuiPalette.ACCENT_DARK, 0.18f), openEase * reveal)
        g2d.fillRoundedRect(setX, y + 4f, settingsWidth - 10f, ctrlH, ctrlH / 2f)
        textDraws.add(TextDraw("SET", (setX + (settingsWidth - 10 - font.width("SET")) / 2).toInt(), y + (rowHeight - 8) / 2, scaleAlpha(ClickGuiPalette.MUTED, openEase * reveal)))
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = toUiX(mouseButtonEvent.x).toInt()
        val my = toUiY(mouseButtonEvent.y).toInt()

        if (searchBox.isMouseOver(mx.toDouble(), my.toDouble())) {
            searchBox.isFocused = true
            return searchBox.mouseClicked(mouseButtonEvent, bl)
        }
        searchBox.isFocused = false

        // サイドバークリック
        if (mx in padding..(padding + sidebarWidth)) {
            val index = rowIndexFromMouse(my.toDouble(), padding + searchHeight + searchGap + headerHeight + padding / 2, height, categoryScroll)
            if (index in categories.indices) {
                selectCategory(categories[index])
                return true
            }
        }

        // コンテンツクリック
        val contentX = padding * 2 + sidebarWidth
        if (mx in contentX..(width - padding)) {
            val index = rowIndexFromMouse(my.toDouble(), padding + searchHeight + searchGap + headerHeight, height, featureScroll)
            if (index in featureEntries.indices) {
                val (_, feature) = featureEntries[index]
                if (mx > width - padding - settingsWidth) {
                    openFeatureSettings(feature) // Registryを介さず抽象メソッドを呼ぶ
                } else {
                    if (feature.isEnabled()) feature.disable() else feature.enable()
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

    private fun totalRowsHeight(count: Int): Double = if (count <= 0) 0.0 else count * (rowHeight + rowGap) - rowGap.toDouble()
    private fun clamp(value: Double, max: Double): Double = value.coerceIn(0.0, max)
    private fun scaleAlpha(color: Int, factor: Float): Int = color.alpha(((color ushr 24) * factor).toInt())
    private fun updateHover(state: MutableList<Float>, index: Int, target: Float, speed: Float): Float {
        ensureSize(state, index + 1)
        state[index] += (target - state[index]) * speed
        return state[index]
    }
    private fun updateFeatureEnable(feature: Feature, target: Float): Float {
        featureEnable[feature] = (featureEnable[feature] ?: target) + (target - (featureEnable[feature] ?: target)) * 0.18f
        return featureEnable[feature]!!
    }
    private fun ensureSize(state: MutableList<Float>, size: Int) {
        while (state.size < size) state.add(0f)
    }
    private fun rowIndexFromMouse(mouseY: Double, listY: Int, listH: Int, scroll: Double): Int = if (mouseY < listY || mouseY > listY + listH) -1 else ((mouseY - listY + scroll) / (rowHeight + rowGap)).toInt()
    private fun uiOffsetX(): Float = (width - width * uiScale) / 2f
    private fun uiOffsetY(): Float = (height - height * uiScale) / 2f
    private fun toUiX(x: Double): Double = (x - uiOffsetX()) / uiScale
    private fun toUiY(y: Double): Double = (y - uiOffsetY()) / uiScale
}
