package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.infinite.ui.ClickGuiPalette
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.fillRoundedRect
import org.infinite.utils.mix
import org.lwjgl.glfw.GLFW
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

abstract class ClickGuiScreen(
    title: Component,
    private val parent: Screen? = null,
) : Screen(title) {

    protected abstract val categories: List<Category<*, out Feature>>

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

    private var selectedCategory: Category<*, out Feature>? = null
    private var searchQuery: String = ""
    private var featureEntries: List<Pair<Category<*, out Feature>, Feature>> = emptyList()

    private var categoryScroll = 0.0
    private var categoryScrollTarget = 0.0
    private var featureScroll = 0.0
    private var featureScrollTarget = 0.0

    private lateinit var searchBox: EditBox
    private data class TextDraw(val text: String, val x: Int, val y: Int, val color: Int)

    override fun init() {
        clearWidgets()

        val available = categories
        if (available.isNotEmpty() && selectedCategory !in available) {
            selectedCategory = available.first()
        }

        buildSearchBox()
        rebuildFeatureList(true)
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // No-op to prevent the default background from covering the custom panels.
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val panelTop = padding + searchHeight + searchGap
        val sidebarX = padding
        val sidebarY = panelTop
        val sidebarH = height - panelTop - padding
        val contentX = padding * 2 + sidebarWidth
        val contentY = panelTop
        val contentW = width - contentX - padding
        val contentH = height - panelTop - padding

        val pulse = ((sin(System.currentTimeMillis() / 240.0) + 1.0) * 0.5).toFloat()

        val g2d = Graphics2DRenderer(guiGraphics)
        val textDraws = mutableListOf<TextDraw>()
        g2d.fillStyle = ClickGuiPalette.BACKDROP
        g2d.fillRoundedRect(0f, 0f, width.toFloat(), height.toFloat(), 0f)

        g2d.fillStyle = ClickGuiPalette.PANEL
        g2d.fillRoundedRect(sidebarX.toFloat(), sidebarY.toFloat(), sidebarWidth.toFloat(), sidebarH.toFloat(), panelRadius)
        g2d.fillRoundedRect(contentX.toFloat(), contentY.toFloat(), contentW.toFloat(), contentH.toFloat(), panelRadius)

        g2d.fillStyle = ClickGuiPalette.PANEL_ALT
        g2d.fillRoundedRect(sidebarX.toFloat(), sidebarY.toFloat(), sidebarWidth.toFloat(), headerHeight.toFloat(), panelRadius)
        g2d.fillRoundedRect(contentX.toFloat(), contentY.toFloat(), contentW.toFloat(), headerHeight.toFloat(), panelRadius)

        updateScrollTargets()
        categoryScroll += (categoryScrollTarget - categoryScroll) * 0.35
        featureScroll += (featureScrollTarget - featureScroll) * 0.35

        val searchX = searchBox.x.toFloat()
        val searchY = searchBox.y.toFloat()
        val searchW = searchBox.width.toFloat()
        val searchH = searchBox.height.toFloat()
        val searchRadius = (searchH * 0.5f).coerceAtLeast(6f)
        g2d.fillStyle = ClickGuiPalette.PANEL_ALT
        g2d.fillRoundedRect(searchX, searchY, searchW, searchH, searchRadius)

        val font = minecraft.font
        textDraws.add(TextDraw("Categories", sidebarX + 8, sidebarY + 7, ClickGuiPalette.MUTED))
        textDraws.add(TextDraw(title.string, contentX + 8, contentY + 7, ClickGuiPalette.MUTED))

        val categoryListX = sidebarX + padding
        val categoryListY = sidebarY + headerHeight + padding / 2
        val categoryListW = sidebarWidth - padding * 2
        val categoryListH = sidebarH - headerHeight - padding
        renderCategoryRows(g2d, font, textDraws, mouseX, mouseY, categoryListX, categoryListY, categoryListW, categoryListH)

        val featureListX = contentX + padding
        val featureListY = contentY + headerHeight
        val featureListW = contentW - padding * 2
        val featureListH = contentH - headerHeight - padding
        renderFeatureRows(g2d, font, textDraws, mouseX, mouseY, pulse, featureListX, featureListY, featureListW, featureListH)

        g2d.flush()

        textDraws.forEach { draw ->
            guiGraphics.drawString(font, draw.text, draw.x, draw.y, draw.color, false)
        }

        super.render(guiGraphics, mouseX, mouseY, delta)

        if (featureEntries.isEmpty()) {
            val message = if (searchQuery.isBlank()) "No modules in this category." else "No matches."
            val messageX = contentX + padding + 6
            val messageY = contentY + headerHeight + 8
            guiGraphics.drawString(font, message, messageX, messageY, ClickGuiPalette.MUTED, false)
        }
    }

    private fun buildSearchBox() {
        val searchX = padding
        val searchW = (width - padding * 2).coerceAtLeast(140)
        val searchY = padding

        searchBox = EditBox(minecraft.font, searchX, searchY, searchW, searchHeight, Component.literal("Search")).apply {
            setMaxLength(60)
            setBordered(false)
            setTextColor(ClickGuiPalette.TEXT)
            value = searchQuery
            setResponder {
                searchQuery = it
                rebuildFeatureList(true)
            }
        }
        addRenderableWidget(searchBox)
    }

    private fun selectCategory(category: Category<*, out Feature>) {
        if (selectedCategory == category) return
        selectedCategory = category
        if (searchQuery.isBlank()) {
            rebuildFeatureList(true)
        }
    }

    private fun rebuildFeatureList(resetScroll: Boolean) {
        val query = searchQuery.trim()
        featureEntries = if (query.isBlank()) {
            selectedCategory?.features?.values?.map { selectedCategory!! to it } ?: emptyList()
        } else {
            categories.flatMap { category ->
                category.features.values.map { category to it }
            }.filter { (_, feature) ->
                feature.name.contains(query, ignoreCase = true) ||
                    Component.translatable(feature.translation()).string.contains(query, ignoreCase = true)
            }
        }

        if (resetScroll) {
            featureScroll = 0.0
            featureScrollTarget = 0.0
        }
    }

    private fun renderCategoryRows(
        g2d: Graphics2DRenderer,
        font: net.minecraft.client.gui.Font,
        textDraws: MutableList<TextDraw>,
        mouseX: Int,
        mouseY: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val stride = rowHeight + rowGap
        val listTop = y.toDouble()
        val listBottom = (y + height).toDouble()
        val scroll = categoryScroll

        categories.forEachIndexed { index, category ->
            val rowTop = listTop + index * stride - scroll
            val rowBottom = rowTop + rowHeight
            if (rowBottom < listTop || rowTop > listBottom) return@forEachIndexed

            val selected = category == selectedCategory
            val hovered = mouseX in x..(x + width) && mouseY.toDouble() in rowTop..rowBottom
            val baseFill = if (selected) ClickGuiPalette.ACCENT_DARK else ClickGuiPalette.PANEL_ALT
            val fill = if (hovered) baseFill.mix(ClickGuiPalette.HOVER, 0.25f) else baseFill

            val rowX = x.toFloat()
            val rowY = rowTop.toFloat()
            g2d.fillStyle = fill
            g2d.fillRoundedRect(rowX, rowY, width.toFloat(), rowHeight.toFloat(), rowRadius)

            if (selected) {
                g2d.fillStyle = ClickGuiPalette.ACCENT
                g2d.fillRoundedRect(rowX, rowY, 3f, rowHeight.toFloat(), 2f)
            }

            val label = Component.translatable(category.translation()).string
            val textY = rowY + (rowHeight - 8) / 2f
            textDraws.add(TextDraw(label, x + 8, textY.toInt(), ClickGuiPalette.TEXT))
        }
    }

    private fun renderFeatureRows(
        g2d: Graphics2DRenderer,
        font: net.minecraft.client.gui.Font,
        textDraws: MutableList<TextDraw>,
        mouseX: Int,
        mouseY: Int,
        pulse: Float,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val stride = rowHeight + rowGap
        val listTop = y.toDouble()
        val listBottom = (y + height).toDouble()
        val scroll = featureScroll

        featureEntries.forEachIndexed { index, (category, feature) ->
            val rowTop = listTop + index * stride - scroll
            val rowBottom = rowTop + rowHeight
            if (rowBottom < listTop || rowTop > listBottom) return@forEachIndexed

            val enabled = feature.isEnabled()
            val baseFill = if (enabled) {
                ClickGuiPalette.ACCENT_DARK.mix(ClickGuiPalette.ACCENT, 0.12f * pulse)
            } else {
                ClickGuiPalette.PANEL_ALT
            }
            val hovered = mouseX in x..(x + width) && mouseY.toDouble() in rowTop..rowBottom
            val fill = if (hovered) baseFill.mix(ClickGuiPalette.HOVER, 0.2f) else baseFill

            val rowX = x.toFloat()
            val rowY = rowTop.toFloat()
            g2d.fillStyle = fill
            g2d.fillRoundedRect(rowX, rowY, width.toFloat(), rowHeight.toFloat(), rowRadius)

            val barColor = if (enabled) ClickGuiPalette.ACCENT else ClickGuiPalette.BORDER
            g2d.fillStyle = barColor
            g2d.fillRoundedRect(rowX, rowY, 3f, rowHeight.toFloat(), 2f)

            val featureLabel = Component.translatable(feature.translation()).string
            val name = if (searchQuery.isBlank()) {
                featureLabel
            } else {
                val label = Component.translatable(category.translation()).string
                "$featureLabel [$label]"
            }

            val textY = rowY + (rowHeight - 8) / 2f
            textDraws.add(TextDraw(name, x + 8, textY.toInt(), ClickGuiPalette.TEXT))

            val stateText = if (enabled) "ON" else "OFF"
            val stateColor = if (enabled) ClickGuiPalette.ACCENT else ClickGuiPalette.MUTED
            val stateWidth = font.width(stateText)
            val stateX = x + width - settingsWidth - stateWidth - 10
            textDraws.add(TextDraw(stateText, stateX, textY.toInt(), stateColor))

            val setText = "SET"
            val setX = x + width - settingsWidth + (settingsWidth - font.width(setText)) / 2
            textDraws.add(TextDraw(setText, setX, textY.toInt(), ClickGuiPalette.MUTED))
        }
    }

    private fun updateScrollTargets() {
        val categoryListH = height - (padding + searchHeight + searchGap) - padding - headerHeight - padding
        val categoryMax = max(0.0, totalRowsHeight(categories.size) - categoryListH)
        categoryScrollTarget = clamp(categoryScrollTarget, categoryMax)
        if (abs(categoryScrollTarget - categoryScroll) < 0.1) categoryScroll = categoryScrollTarget

        val featureListH = height - (padding + searchHeight + searchGap) - padding - headerHeight - padding
        val featureMax = max(0.0, totalRowsHeight(featureEntries.size) - featureListH)
        featureScrollTarget = clamp(featureScrollTarget, featureMax)
        if (abs(featureScrollTarget - featureScroll) < 0.1) featureScroll = featureScrollTarget
    }

    private fun totalRowsHeight(count: Int): Double {
        if (count <= 0) return 0.0
        return count * (rowHeight + rowGap) - rowGap.toDouble()
    }

    private fun clamp(value: Double, max: Double): Double = value.coerceIn(0.0, max)

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = mouseButtonEvent.x
        val my = mouseButtonEvent.y
        if (searchBox.isMouseOver(mx, my)) {
            searchBox.setFocused(true)
            searchBox.mouseClicked(mouseButtonEvent, bl)
            return true
        }
        searchBox.setFocused(false)
        val sidebarX = padding
        val sidebarY = padding + searchHeight + searchGap
        val sidebarH = height - sidebarY - padding

        val inSidebar = mx >= sidebarX && mx <= sidebarX + sidebarWidth &&
            my >= sidebarY && my <= sidebarY + sidebarH
        if (inSidebar) {
            val listY = sidebarY + headerHeight + padding / 2
            val listH = sidebarH - headerHeight - padding
            val index = rowIndexFromMouse(my, listY, listH, categoryScroll)
            if (index in categories.indices) {
                val rowTop = listY + index * (rowHeight + rowGap) - categoryScroll
                if (my <= rowTop + rowHeight) {
                    selectCategory(categories[index])
                    return true
                }
            }
        }

        val contentX = padding * 2 + sidebarWidth
        val contentY = padding + searchHeight + searchGap
        val contentW = width - contentX - padding
        val contentH = height - contentY - padding
        val inContent = mx >= contentX && mx <= contentX + contentW &&
            my >= contentY && my <= contentY + contentH
        if (inContent) {
            val listY = contentY + headerHeight
            val listH = contentH - headerHeight - padding
            val index = rowIndexFromMouse(my, listY, listH, featureScroll)
            if (index in featureEntries.indices) {
                val rowTop = listY + index * (rowHeight + rowGap) - featureScroll
                if (my <= rowTop + rowHeight) {
                    val (_, feature) = featureEntries[index]
                    val localX = mx - (contentX + padding)
                    val openSettings = mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT ||
                        localX >= contentW - padding - settingsWidth
                    if (openSettings) {
                        minecraft.setScreen(FeatureScreen(feature, this))
                    } else if (mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        if (feature.isEnabled()) feature.disable() else feature.enable()
                    }
                    return true
                }
            }
        }

        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (searchBox.isFocused) {
            if (keyEvent.key == GLFW.GLFW_KEY_ESCAPE) {
                searchBox.setFocused(false)
                return true
            }
            if (searchBox.keyPressed(keyEvent)) {
                return true
            }
        }
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (searchBox.charTyped(characterEvent)) {
            return true
        }
        return super.charTyped(characterEvent)
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        val mx = d
        val my = e
        val scrollDelta = -g * (rowHeight + rowGap)

        val sidebarX = padding
        val sidebarY = padding + searchHeight + searchGap
        val sidebarH = height - sidebarY - padding
        val inSidebar = mx >= sidebarX && mx <= sidebarX + sidebarWidth &&
            my >= sidebarY && my <= sidebarY + sidebarH
        if (inSidebar) {
            val maxScroll = max(0.0, totalRowsHeight(categories.size) - (sidebarH - headerHeight - padding))
            categoryScrollTarget = clamp(categoryScrollTarget + scrollDelta, maxScroll)
            return true
        }

        val contentX = padding * 2 + sidebarWidth
        val contentY = padding + searchHeight + searchGap
        val contentW = width - contentX - padding
        val contentH = height - contentY - padding
        val inContent = mx >= contentX && mx <= contentX + contentW &&
            my >= contentY && my <= contentY + contentH
        if (inContent) {
            val maxScroll = max(0.0, totalRowsHeight(featureEntries.size) - (contentH - headerHeight - padding))
            featureScrollTarget = clamp(featureScrollTarget + scrollDelta, maxScroll)
            return true
        }

        return super.mouseScrolled(d, e, f, g)
    }

    private fun rowIndexFromMouse(mouseY: Double, listY: Int, listH: Int, scroll: Double): Int {
        if (mouseY < listY || mouseY > listY + listH) return -1
        val relativeY = mouseY - listY + scroll
        val stride = rowHeight + rowGap
        return (relativeY / stride).toInt()
    }
}
