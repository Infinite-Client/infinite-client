package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.infinite.ui.ClickGuiPalette
import org.infinite.infinite.ui.UiStyleRegistry
import org.infinite.libs.core.features.Category
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
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
    private val uiScale = 1.0f

    private var selectedCategory: Category<*, out Feature>? = null
    private var searchQuery: String = ""
    private var featureEntries: List<Pair<Category<*, out Feature>, Feature>> = emptyList()

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

    override fun init() {
        clearWidgets()
        openTime = System.currentTimeMillis()
        searchFocus = 0f
        categoryHover.clear()
        featureHover.clear()
        featureEnable.clear()

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
        val uiMouseX = toUiX(mouseX.toDouble()).toInt()
        val uiMouseY = toUiY(mouseY.toDouble()).toInt()
        val panelTop = padding + searchHeight + searchGap
        val sidebarX = padding
        val sidebarY = panelTop
        val sidebarH = height - panelTop - padding
        val contentX = padding * 2 + sidebarWidth
        val contentY = panelTop
        val contentW = width - contentX - padding
        val contentH = height - panelTop - padding

        val pulse = ((sin(System.currentTimeMillis() / 240.0) + 1.0) * 0.5).toFloat()

        openProgress = ((System.currentTimeMillis() - openTime) / 220f).coerceIn(0f, 1f)
        openEase = openProgress * openProgress * (3f - 2f * openProgress)
        val backdrop = Graphics2DRenderer(guiGraphics)
        backdrop.fillStyle = scaleAlpha(ClickGuiPalette.BACKDROP, openEase)
        backdrop.fillRoundedRect(0f, 0f, width.toFloat(), height.toFloat(), 0f)
        backdrop.flush()

        guiGraphics.pose().pushMatrix()
        guiGraphics.pose().translate(uiOffsetX(), uiOffsetY())
        guiGraphics.pose().scale(uiScale, uiScale)

        val g2d = Graphics2DRenderer(guiGraphics)
        val textDraws = mutableListOf<TextDraw>()

        g2d.fillStyle = scaleAlpha(ClickGuiPalette.PANEL, openEase)
        g2d.fillRoundedRect(sidebarX.toFloat(), sidebarY.toFloat(), sidebarWidth.toFloat(), sidebarH.toFloat(), panelRadius)
        g2d.fillRoundedRect(contentX.toFloat(), contentY.toFloat(), contentW.toFloat(), contentH.toFloat(), panelRadius)

        g2d.fillStyle = scaleAlpha(ClickGuiPalette.PANEL_ALT, openEase)
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
        val focusTarget = if (searchBox.isFocused) 1f else 0f
        searchFocus += (focusTarget - searchFocus) * 0.25f
        val searchFill = ClickGuiPalette.PANEL_ALT.mix(ClickGuiPalette.ACCENT_DARK, 0.2f * searchFocus)
        g2d.fillStyle = scaleAlpha(searchFill, openEase)
        g2d.fillRoundedRect(searchX, searchY, searchW, searchH, searchRadius)
        if (searchFocus > 0.01f) {
            val glowAlpha = (70 * searchFocus * openEase).toInt()
            g2d.fillStyle = ClickGuiPalette.ACCENT.alpha(glowAlpha)
            g2d.fillRoundedRect(
                searchX - 2f,
                searchY - 2f,
                searchW + 4f,
                searchH + 4f,
                searchRadius + 2f,
            )
        }

        val font = minecraft.font
        val headerColor = scaleAlpha(ClickGuiPalette.MUTED, openEase)
        textDraws.add(TextDraw("Categories", sidebarX + 8, sidebarY + 7, headerColor))
        textDraws.add(TextDraw(title.string, contentX + 8, contentY + 7, headerColor))

        val categoryListX = sidebarX + padding
        val categoryListY = sidebarY + headerHeight + padding / 2
        val categoryListW = sidebarWidth - padding * 2
        val categoryListH = sidebarH - headerHeight - padding
        renderCategoryRows(g2d, font, textDraws, uiMouseX, uiMouseY, categoryListX, categoryListY, categoryListW, categoryListH)

        val featureListX = contentX + padding
        val featureListY = contentY + headerHeight
        val featureListW = contentW - padding * 2
        val featureListH = contentH - headerHeight - padding
        renderFeatureRows(g2d, font, textDraws, uiMouseX, uiMouseY, pulse, featureListX, featureListY, featureListW, featureListH)

        g2d.flush()

        textDraws.forEach { draw ->
            guiGraphics.drawString(font, draw.text, draw.x, draw.y, draw.color, false)
        }

        super.render(guiGraphics, uiMouseX, uiMouseY, delta)

        if (featureEntries.isEmpty()) {
            val message = if (searchQuery.isBlank()) "No modules in this category." else "No matches."
            val messageX = contentX + padding + 6
            val messageY = contentY + headerHeight + 8
            guiGraphics.drawString(font, message, messageX, messageY, ClickGuiPalette.MUTED, false)
        }

        guiGraphics.pose().popMatrix()
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
                feature.name.contains(query, ignoreCase = true)
            }
        }
        val currentFeatures = featureEntries.map { it.second }.toSet()
        featureEnable.keys.retainAll(currentFeatures)

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
        ensureSize(categoryHover, categories.size)
        val stride = rowHeight + rowGap
        val listTop = y.toDouble()
        val listBottom = (y + height).toDouble()
        val scroll = categoryScroll

        categories.forEachIndexed { index, category ->
            val reveal = ((openProgress * 1.4f) - index * 0.05f).coerceIn(0f, 1f)
            val rowAlpha = (openEase * reveal).coerceIn(0f, 1f)
            val rowTop = listTop + index * stride - scroll
            val rowBottom = rowTop + rowHeight
            if (rowBottom < listTop || rowTop > listBottom) return@forEachIndexed

            val selected = category == selectedCategory
            val hovered = mouseX in x..(x + width) && mouseY.toDouble() in rowTop..rowBottom
            val baseFill = if (selected) ClickGuiPalette.ACCENT_DARK else ClickGuiPalette.PANEL_ALT
            val hover = updateHover(categoryHover, index, if (hovered) 1f else 0f, 0.2f)
            val fill = baseFill.mix(ClickGuiPalette.HOVER, 0.25f * hover)

            val rowX = x.toFloat()
            val rowY = rowTop.toFloat()
            g2d.fillStyle = scaleAlpha(fill, rowAlpha)
            g2d.fillRoundedRect(rowX, rowY, width.toFloat(), rowHeight.toFloat(), rowRadius)

            if (selected) {
                g2d.fillStyle = scaleAlpha(ClickGuiPalette.ACCENT, rowAlpha)
                g2d.fillRoundedRect(rowX, rowY, 3f, rowHeight.toFloat(), 2f)
            }

            val label = category.name
            val textY = rowY + (rowHeight - 8) / 2f
            textDraws.add(TextDraw(label, x + 8, textY.toInt(), scaleAlpha(ClickGuiPalette.TEXT, rowAlpha)))
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
        ensureSize(featureHover, featureEntries.size)
        val stride = rowHeight + rowGap
        val listTop = y.toDouble()
        val listBottom = (y + height).toDouble()
        val scroll = featureScroll

        featureEntries.forEachIndexed { index, (category, feature) ->
            val reveal = ((openProgress * 1.3f) - index * 0.035f).coerceIn(0f, 1f)
            val rowAlpha = (openEase * reveal).coerceIn(0f, 1f)
            val rowTop = listTop + index * stride - scroll
            val rowBottom = rowTop + rowHeight
            if (rowBottom < listTop || rowTop > listBottom) return@forEachIndexed

            val enabledTarget = if (feature.isEnabled()) 1f else 0f
            val enabledValue = updateFeatureEnable(feature, enabledTarget)
            val activeTint = ClickGuiPalette.ACCENT_DARK.mix(ClickGuiPalette.ACCENT, 0.4f)
            val baseFill = ClickGuiPalette.PANEL_ALT.mix(activeTint, 0.22f * enabledValue + 0.08f * pulse * enabledValue)
            val hovered = mouseX in x..(x + width) && mouseY.toDouble() in rowTop..rowBottom
            val hover = updateHover(featureHover, index, if (hovered) 1f else 0f, 0.2f)
            val fill = baseFill.mix(ClickGuiPalette.HOVER, 0.2f * hover)

            val rowX = x.toFloat()
            val rowY = rowTop.toFloat()
            g2d.fillStyle = scaleAlpha(fill, rowAlpha)
            g2d.fillRoundedRect(rowX, rowY, width.toFloat(), rowHeight.toFloat(), rowRadius)

            val barColor = ClickGuiPalette.BORDER.mix(ClickGuiPalette.ACCENT, enabledValue)
            g2d.fillStyle = scaleAlpha(barColor, rowAlpha)
            g2d.fillRoundedRect(rowX, rowY, 3f, rowHeight.toFloat(), 2f)

            val featureLabel = feature.name
            val name = if (searchQuery.isBlank()) {
                featureLabel
            } else {
                val label = category.name
                "$featureLabel [$label]"
            }

            val textY = rowY + (rowHeight - 8) / 2f
            textDraws.add(TextDraw(name, x + 8, textY.toInt(), scaleAlpha(ClickGuiPalette.TEXT, rowAlpha)))

            val controlH = rowHeight.toFloat() - 8f
            val controlY = rowY + (rowHeight - controlH) / 2f
            val setW = settingsWidth.toFloat() - 10f
            val setX = rowX + width - settingsWidth + 5f
            val setY = controlY
            val toggleW = 36f
            val toggleX = setX - toggleW - 8f
            val toggleY = controlY
            val settingsHovered = mouseX.toFloat() in setX..(setX + setW) && mouseY.toDouble() in rowTop..rowBottom
            val toggleHovered = mouseX.toFloat() in toggleX..(toggleX + toggleW) && mouseY.toDouble() in rowTop..rowBottom

            val toggleBase = ClickGuiPalette.PANEL_ALT.mix(ClickGuiPalette.ACCENT_DARK, 0.25f * enabledValue)
            val toggleFill = if (toggleHovered) toggleBase.mix(ClickGuiPalette.HOVER, 0.3f) else toggleBase
            g2d.fillStyle = scaleAlpha(toggleFill, rowAlpha)
            g2d.fillRoundedRect(toggleX, toggleY, toggleW, controlH, controlH / 2f)

            val knobSize = controlH - 4f
            val knobX = toggleX + 2f + (toggleW - knobSize - 4f) * enabledValue
            val knobColor = ClickGuiPalette.TEXT.mix(ClickGuiPalette.MUTED, 1f - enabledValue)
            g2d.fillStyle = scaleAlpha(knobColor, rowAlpha)
            g2d.fillRoundedRect(knobX, toggleY + 2f, knobSize, knobSize, knobSize / 2f)

            val setBase = ClickGuiPalette.PANEL_ALT.mix(ClickGuiPalette.ACCENT_DARK, 0.18f)
            val setFill = if (settingsHovered) setBase.mix(ClickGuiPalette.ACCENT, 0.35f) else setBase
            g2d.fillStyle = scaleAlpha(setFill, rowAlpha)
            g2d.fillRoundedRect(setX, setY, setW, controlH, controlH / 2f)

            val stateText = if (enabledValue > 0.5f) "ON" else "OFF"
            val stateColor = ClickGuiPalette.MUTED.mix(ClickGuiPalette.TEXT, 0.35f + 0.45f * enabledValue)
            val stateWidth = font.width(stateText)
            val stateX = (toggleX - stateWidth - 6f).toInt()
            textDraws.add(TextDraw(stateText, stateX, textY.toInt(), scaleAlpha(stateColor, rowAlpha)))

            val setText = "SET"
            val setTextX = (setX + (setW - font.width(setText)) / 2f).toInt()
            val setColor = if (settingsHovered) ClickGuiPalette.TEXT else ClickGuiPalette.MUTED
            textDraws.add(TextDraw(setText, setTextX, textY.toInt(), scaleAlpha(setColor, rowAlpha)))
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

    private fun scaleAlpha(color: Int, factor: Float): Int {
        val baseAlpha = color ushr 24
        return color.alpha((baseAlpha * factor).toInt())
    }

    private fun updateHover(state: MutableList<Float>, index: Int, target: Float, speed: Float): Float {
        ensureSize(state, index + 1)
        val current = state[index]
        val next = current + (target - current) * speed
        state[index] = next
        return next
    }

    private fun updateFeatureEnable(feature: Feature, target: Float): Float {
        val current = featureEnable[feature] ?: target
        val next = current + (target - current) * 0.18f
        featureEnable[feature] = next
        return next
    }

    private fun ensureSize(state: MutableList<Float>, size: Int) {
        if (state.size < size) {
            repeat(size - state.size) { state.add(0f) }
        } else if (state.size > size) {
            state.subList(size, state.size).clear()
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = toUiX(mouseButtonEvent.x)
        val my = toUiY(mouseButtonEvent.y)
        val scaledEvent = MouseButtonEvent(mx, my, mouseButtonEvent.buttonInfo)
        if (searchBox.isMouseOver(mx, my)) {
            searchBox.setFocused(true)
            searchBox.mouseClicked(scaledEvent, bl)
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
                        val style = InfiniteClient.globalFeatures.rendering.uiStyleFeature.style.value
                        UiStyleRegistry.provider(style).openFeatureSettings(feature, this)
                    } else if (mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        if (feature.isEnabled()) feature.disable() else feature.enable()
                    }
                    return true
                }
            }
        }

        return super.mouseClicked(scaledEvent, bl)
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
        val mx = toUiX(d)
        val my = toUiY(e)
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

        return super.mouseScrolled(mx, my, f, g)
    }

    private fun rowIndexFromMouse(mouseY: Double, listY: Int, listH: Int, scroll: Double): Int {
        if (mouseY < listY || mouseY > listY + listH) return -1
        val relativeY = mouseY - listY + scroll
        val stride = rowHeight + rowGap
        return (relativeY / stride).toInt()
    }

    private fun uiOffsetX(): Float = (width - width * uiScale) / 2f
    private fun uiOffsetY(): Float = (height - height * uiScale) / 2f
    private fun toUiX(x: Double): Double = (x - uiOffsetX()) / uiScale
    private fun toUiY(y: Double): Double = (y - uiOffsetY()) / uiScale
}

