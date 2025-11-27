package org.infinite.gui.screen

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import net.minecraft.util.math.ColorHelper
import org.infinite.ConfigManager
import org.infinite.Feature
import org.infinite.InfiniteClient
import org.infinite.gui.widget.FeatureSearchWidget
import org.infinite.gui.widget.InfiniteButton
import org.infinite.gui.widget.InfiniteFeatureToggle
import org.infinite.gui.widget.InfiniteScrollableContainer
import org.infinite.utils.rendering.drawBorder
import org.infinite.utils.rendering.transparent

class UISection(
    val id: String,
    private val screen: Screen,
    featureList: List<Feature>? = null,
) {
    private var closeButton: InfiniteButton? = null
    val widgets = mutableListOf<ClickableWidget>()
    private var featureSearchWidget: FeatureSearchWidget? = null
    private var isMainSectionInitialized = false
    private var themeTiles: List<ThemeTile> = emptyList()
    private var themeScrollOffset: Double = 0.0
    private var themeAreaX: Int = 0
    private var themeAreaY: Int = 0
    private var themeAreaWidth: Int = 0
    private var themeAreaHeight: Int = 0
    private var themeContentHeight: Int = 0
    private var reloadButton: InfiniteButton? = null

    private data class ThemeTile(
        val name: String,
        var x: Int,
        var y: Int,
        var width: Int,
        val height: Int,
    )

    init {
        when (id) {
            "main" -> {
                // Initialization moved to renderMain
            }

            else -> {
                featureList?.let {
                    setupFeatureWidgets(it)
                }
            }
        }
    }

    private fun setupFeatureWidgets(features: List<Feature>) {
        val featureWidgets =
            features.map { feature ->
                feature.name
                InfiniteFeatureToggle(0, 0, 280, 20, feature, false) {
                    MinecraftClient.getInstance().setScreen(FeatureSettingsScreen(screen, feature))
                }
            }

        if (featureWidgets.isNotEmpty()) {
            val container = InfiniteScrollableContainer(0, 0, 300, 180, featureWidgets.toMutableList())
            widgets.add(container)
        }
    }

    fun render(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        isSelected: Boolean,
        textRenderer: TextRenderer,
        borderColor: Int,
        alpha: Int,
        renderContent: Boolean,
    ) {
        // Draw the icon in the center of the panel
        val icon = InfiniteClient.theme().icon
        if (icon != null) {
            val iconWidth = if (icon.width > icon.height) 256 else 256 * icon.width / icon.height
            val iconHeight = if (icon.width < icon.height) 256 else 256 * icon.height / icon.width
            val iconX = x + (width - iconWidth) / 2
            val iconY = y + (height - iconHeight) / 2
            val iconColor =
                borderColor.transparent(128)
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                icon.identifier,
                iconX,
                iconY,
                0f,
                0f,
                iconWidth,
                iconHeight,
                icon.width,
                icon.height,
                icon.width,
                icon.height,
                iconColor,
            )
        }
        val backgroundColor =
            InfiniteClient
                .theme()
                .colors.backgroundColor
                .transparent(alpha)
        context.drawBorder(x, y, width, height, borderColor)
        context.fill(x, y, x + width, y + height, backgroundColor)

        val titleText =
            when (id) {
                "main" -> "Main"
                else ->
                    id
                        .replace("-settings", "")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } +
                        " Settings"
            }

        if (id == "main") {
            renderMain(context, x, y, width, height, textRenderer, isSelected, mouseX, mouseY, delta, renderContent)
        } else {
            renderSettings(
                context,
                x,
                y,
                width,
                height,
                textRenderer,
                titleText,
                isSelected,
                mouseX,
                mouseY,
                delta,
                renderContent,
            )
        }

        if (isSelected && renderContent) {
            if (closeButton == null || closeButton?.x != x + width - 30 || closeButton?.y != y + 10) {
                closeButton =
                    InfiniteButton(
                        x = x + width - 30,
                        y = y + 10,
                        width = 20,
                        height = 20,
                        message = Text.literal("X"),
                    ) {
                        screen.close()
                    }
            }
            closeButton?.render(context, mouseX, mouseY, delta)
        } else {
            closeButton = null
        }
    }

    private fun renderMain(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        textRenderer: TextRenderer,
        isSelected: Boolean,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        renderContent: Boolean,
    ) {
        renderTitle(context, x, y, width, textRenderer, "Main", isSelected)
        if (!renderContent) return

        val chipStartX = x + 20
        val chipStartY = y + 48
        val chipAreaWidth = width - 40
        val minThemeHeight = kotlin.math.min(120, height / 2)
        val maxThemeHeight = kotlin.math.max(120, height / 2)
        val themeAreaHeightLocal = (height * 0.35f).toInt().coerceIn(minThemeHeight, maxThemeHeight)
        themeAreaX = chipStartX
        themeAreaY = chipStartY
        themeAreaWidth = chipAreaWidth
        themeAreaHeight = themeAreaHeightLocal

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Themes"),
            chipStartX,
            chipStartY - 14,
            InfiniteClient.theme().colors.foregroundColor,
        )
        ensureReloadButton(chipStartX + chipAreaWidth - 70, chipStartY - 18)
        reloadButton?.render(context, mouseX, mouseY, delta)

        val tilesBottom = layoutThemeTiles(chipStartX, chipStartY, chipAreaWidth, textRenderer)
        themeContentHeight = tilesBottom - chipStartY
        val maxScroll = (themeContentHeight - themeAreaHeight).coerceAtLeast(0)
        if (themeScrollOffset > maxScroll) themeScrollOffset = maxScroll.toDouble()

        context.enableScissor(chipStartX, chipStartY, chipStartX + chipAreaWidth, chipStartY + themeAreaHeight)
        drawThemeTiles(context, textRenderer, mouseX, mouseY, chipStartY, themeAreaHeight)
        context.disableScissor()

        if (!isMainSectionInitialized) {
            featureSearchWidget =
                FeatureSearchWidget(
                    x + 20,
                    chipStartY + themeAreaHeight + 16,
                    width - 40,
                    height - (themeAreaHeight + (chipStartY - y)) - 32,
                    screen,
                )
            isMainSectionInitialized = true
        }

        featureSearchWidget?.let {
            it.x = x + 20
            it.y = chipStartY + themeAreaHeight + 16
            it.width = width - 40
            it.height = height - (themeAreaHeight + (chipStartY - y)) - 32
            it.render(context, mouseX, mouseY, delta)
        }
    }

    private fun renderSettings(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        textRenderer: TextRenderer,
        titleText: String,
        isSelected: Boolean,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        renderContent: Boolean,
    ) {
        renderTitle(context, x, y, width, textRenderer, titleText, isSelected)
        if (!renderContent) return

        var currentY = y + 50
        widgets.forEach { widget ->
            if (widget is InfiniteScrollableContainer) {
                // スクロールコンテナの位置と高さの再計算
                widget.setPosition(x + (width - widget.width) / 2 - 20, y + 50)
                widget.height = height - 60
            } else {
                widget.x = x + 20
                widget.y = currentY
                currentY += widget.height + 5
            }
            widget.render(context, mouseX, mouseY, delta)
        }
    }

    private fun layoutThemeTiles(
        startX: Int,
        startY: Int,
        availableWidth: Int,
        textRenderer: TextRenderer,
    ): Int {
        val themes = InfiniteClient.themes

        val tileHeight = 40
        val verticalGap = 10
        val horizontalGap = 12
        val tileWidth = ((availableWidth - horizontalGap) / 2).coerceAtLeast(160)

        val tiles = mutableListOf<ThemeTile>()
        var cursorX = startX
        var cursorY = startY
        var column = 0

        for (theme in themes) {
            tiles.add(ThemeTile(theme.name, cursorX, cursorY, tileWidth, tileHeight))
            column++
            if (column >= 2) {
                column = 0
                cursorX = startX
                cursorY += tileHeight + verticalGap
            } else {
                cursorX += tileWidth + horizontalGap
            }
        }
        themeTiles = tiles

        val lastTileBottom = themeTiles.maxOfOrNull { it.y + it.height } ?: startY
        return lastTileBottom
    }

    private fun drawThemeTiles(
        context: DrawContext,
        textRenderer: TextRenderer,
        mouseX: Int,
        mouseY: Int,
        areaTop: Int,
        areaHeight: Int,
    ) {
        val theme = InfiniteClient.theme()
        val textColor = theme.colors.foregroundColor
        val offset = themeScrollOffset.toInt()

        for (tile in themeTiles) {
            val visualY = tile.y - offset
            val isVisible = visualY + tile.height >= areaTop && visualY <= areaTop + areaHeight
            if (!isVisible) continue

            val isCurrent = InfiniteClient.currentTheme.equals(tile.name, ignoreCase = true)
            val hovered = mouseX in tile.x..(tile.x + tile.width) && mouseY in visualY..(visualY + tile.height)

            val baseColor =
                if (isCurrent) {
                    theme.colors.primaryColor.transparent(150)
                } else {
                    theme.colors.backgroundColor.transparent(if (hovered) 180 else 140)
                }
            val borderColor =
                if (isCurrent) {
                    theme.colors.primaryColor
                } else {
                    theme.colors.foregroundColor.transparent(160)
                }

            context.fill(tile.x, visualY, tile.x + tile.width, visualY + tile.height, baseColor)
            context.drawBorder(tile.x, visualY, tile.width, tile.height, borderColor)

            // color swatches
            val swatchSize = 12
            val swatchY = visualY + tile.height - swatchSize - 6
            val swatchStartX = tile.x + tile.width - (swatchSize + 6) * 4
            val colors =
                listOf(
                    theme.colors.backgroundColor,
                    theme.colors.foregroundColor,
                    theme.colors.primaryColor,
                    theme.colors.secondaryColor,
                )
            colors.forEachIndexed { idx, c ->
                val sx = swatchStartX + idx * (swatchSize + 6)
                context.fill(sx, swatchY, sx + swatchSize, swatchY + swatchSize, c.transparent(220))
                context.drawBorder(sx, swatchY, swatchSize, swatchSize, borderColor)
            }

            context.drawTextWithShadow(
                textRenderer,
                Text.literal(tile.name),
                tile.x + 10,
                visualY + 8,
                textColor,
            )
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(if (isCurrent) "Active" else "Click to apply"),
                tile.x + 10,
                visualY + 20,
                textColor.transparent(200),
            )
        }
    }

    private fun ensureReloadButton(
        x: Int,
        y: Int,
    ) {
        if (reloadButton == null) {
            reloadButton =
                InfiniteButton(
                    x,
                    y,
                    64,
                    16,
                    Text.literal("Reload"),
                ) {
                    InfiniteClient.reloadThemes()
                }
        } else {
            reloadButton?.x = x
            reloadButton?.y = y
        }
    }

    private fun renderTitle(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        textRenderer: TextRenderer,
        titleText: String,
        isSelected: Boolean,
    ) {
        val title = Text.of(titleText)
        val textWidth = textRenderer.getWidth(title)
        val textX = x + (width - textWidth) / 2
        val textY = y + 20

        val color =
            if (isSelected) {
                InfiniteClient
                    .theme()
                    .colors.foregroundColor
            } else {
                ColorHelper.getArgb(
                    255,
                    ColorHelper.getRed(
                        InfiniteClient
                            .theme()
                            .colors.foregroundColor,
                    ) / 2,
                    ColorHelper.getGreen(
                        InfiniteClient
                            .theme()
                            .colors.foregroundColor,
                    ) / 2,
                    ColorHelper.getBlue(
                        InfiniteClient
                            .theme()
                            .colors.foregroundColor,
                    ) / 2,
                )
            }
        context.drawTextWithShadow(textRenderer, title, textX, textY, color)
    }

    // ★ 修正点: mouseClicked を Boolean 戻り値に変更し、
    // イベントを処理したウィジェットでループを停止する
    fun mouseClicked(
        click: Click,
        doubled: Boolean,
        isSelected: Boolean,
    ): Boolean { // ★ 戻り値を Boolean に変更
        if (!isSelected) return false

        if (id == "main") {
            if (reloadButton?.mouseClicked(click, doubled) == true) {
                return true
            }

            val clickedTile =
                themeTiles.firstOrNull { tile ->
                    val visualY = tile.y - themeScrollOffset.toInt()
                    click.x >= tile.x && click.x <= tile.x + tile.width &&
                        click.y >= visualY && click.y <= visualY + tile.height
                }
            if (clickedTile != null) {
                val theme = InfiniteClient.themes.find { it.name.equals(clickedTile.name, ignoreCase = true) }
                if (theme != null) {
                    InfiniteClient.currentTheme = theme.name
                    ConfigManager.saveConfig()
                    InfiniteClient.info(Text.translatable("command.infinite.theme.changed", theme.name).string)
                }
                return true
            }

            featureSearchWidget?.mouseClicked(click, doubled)?.let { if (it) return true }
        }

        // 1. closeButtonのクリック
        if (closeButton?.mouseClicked(click, doubled) == true) {
            return true
        }

        // 2. 他のウィジェットのクリック
        for (widget in widgets) {
            if (widget.mouseClicked(click, doubled)) {
                return true // ★ 最初に応答したウィジェットで停止し、フォーカスを与える
            }
        }

        return false
    }

    fun keyPressed(
        input: KeyInput,
        isSelected: Boolean,
    ) {
        if (!isSelected) return

        if (id == "main") {
            featureSearchWidget?.keyPressed(input)
        }

        // keyPressed は一般的に全ての子に転送されます
        widgets.forEach { it.keyPressed(input) }
    }

    fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
        isSelected: Boolean,
    ): Boolean {
        if (!isSelected) return false

        if (id == "main") {
            // theme scroll region bounds
            val themeStartX = themeAreaX
            val themeEndX = themeAreaX + themeAreaWidth
            val themeStartY = themeAreaY
            val themeAreaHeightLocal = themeAreaHeight
            val maxScroll = (themeContentHeight - themeAreaHeightLocal).coerceAtLeast(0)

            if (mouseX in themeStartX.toDouble()..themeEndX.toDouble() &&
                mouseY in themeStartY.toDouble()..(themeStartY + themeAreaHeightLocal).toDouble()
            ) {
                val old = themeScrollOffset
                themeScrollOffset = (themeScrollOffset - verticalAmount * 12).coerceIn(0.0, maxScroll.toDouble())
                if (themeScrollOffset != old) {
                    return true
                }
            }

            featureSearchWidget
                ?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
                ?.let { if (it) return true }
        }

        for (widget in widgets) {
            if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true
            }
        }
        return false
    }

    // ★ 修正点: mouseDragged を全てのウィジェットに転送
    fun mouseDragged(
        click: Click,
        offsetX: Double,
        offsetY: Double,
        isSelected: Boolean,
    ): Boolean { // ★ 戻り値は Boolean
        if (!isSelected) return false

        if (id == "main") {
            featureSearchWidget?.mouseDragged(click, offsetX, offsetY)?.let { if (it) return true }
        }

        // closeButtonへのドラッグを処理
        if (closeButton?.mouseDragged(click, offsetX, offsetY) == true) {
            return true
        }

        // ★ スクロールコンテナとその他のウィジェット（スライダーなど）の両方に転送
        for (widget in widgets) {
            if (widget.mouseDragged(click, offsetX, offsetY)) {
                return true
            }
        }
        return false
    }

    // ★ 修正点: mouseReleased を全てのウィジェットに転送
    fun mouseReleased(
        click: Click,
        isSelected: Boolean,
    ): Boolean { // ★ 戻り値は Boolean
        if (!isSelected) return false

        if (id == "main") {
            if (reloadButton?.mouseReleased(click) == true) {
                return true
            }

            featureSearchWidget?.mouseReleased(click)?.let { if (it) return true }
        }

        // closeButtonの mouseReleased を処理
        if (closeButton?.mouseReleased(click) == true) {
            return true
        }

        // ★ スクロールコンテナとその他のウィジェットの両方に転送
        for (widget in widgets) {
            if (widget.mouseReleased(click)) {
                return true
            }
        }
        return false
    }

    fun charTyped(
        input: CharInput,
        isSelected: Boolean,
    ): Boolean {
        if (!isSelected) return false

        if (id == "main") {
            featureSearchWidget?.charTyped(input)?.let { if (it) return true }
        }

        for (widget in widgets) {
            if (widget.charTyped(input)) {
                return true
            }
        }
        return false
    }
}
