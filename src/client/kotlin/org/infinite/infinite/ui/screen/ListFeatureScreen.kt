package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.ui.layout.ScrollableLayoutContainer
import org.infinite.utils.alpha
import org.lwjgl.glfw.GLFW

class ListFeatureScreen<T : Feature>(
    private val feature: T,
    private val parent: Screen,
) : Screen(Component.literal(feature.name)) {

    private lateinit var container: ScrollableLayoutContainer

    // レイアウト定数
    private val panelRadius = 12f
    private val headerHeight = 36 // 少し広めに調整
    private val panelPadding = 16
    private val screenPadding = 24
    private val scrollbarWidth = 20

    private var panelX = 0
    private var panelY = 0
    private var panelW = 0
    private var panelH = 0

    // テーマ取得用ヘルパー
    private val themeScheme get() = InfiniteClient.theme.colorScheme

    override fun init() {
        clearWidgets()
        feature.ensureAllPropertiesRegistered()

        // パネルサイズの動的計算
        val maxPanelW = (width - screenPadding * 2).coerceAtLeast(200)
        val maxPanelH = (height - screenPadding * 2).coerceAtLeast(180)
        val minPanelW = minOf(320, maxPanelW)
        val minPanelH = minOf(240, maxPanelH)

        panelW = (width * 0.75f).toInt().coerceIn(minPanelW, maxPanelW)
        panelH = (height * 0.82f).toInt().coerceIn(minPanelH, maxPanelH)
        panelX = (width - panelW) / 2
        panelY = (height - panelH) / 2

        val availableWidth = panelW - panelPadding * 2
        val innerWidth = (availableWidth - scrollbarWidth).coerceAtLeast(120)
        val innerLayout = LinearLayout.vertical().spacing(10) // 間隔を少し広げて視認性向上

        // プロパティウィジェットの構築
        feature.properties.forEach { (_, property) ->
            val propertyWidget = property.widget(0, 0, innerWidth)
            innerLayout.addChild(propertyWidget)
        }
        innerLayout.arrangeElements()

        container = ScrollableLayoutContainer(innerLayout, innerWidth).apply {
            x = panelX + panelPadding
            y = panelY + headerHeight + 8 // ヘッダーとの間に少し余白
            setMinWidth(availableWidth)
            setMaxHeight(panelH - headerHeight - panelPadding - 12)
        }
        addRenderableWidget(container)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, delta)
        val g2d = Graphics2DRenderer(guiGraphics)
        InfiniteClient.theme.renderBackGround(0f, 0f, width.toFloat(), height.toFloat(), g2d, 0.1f)
        g2d.flush()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val g2d = Graphics2DRenderer(guiGraphics)

        // 1. パネル本体の描画
        g2d.fillStyle = themeScheme.surfaceColor
        g2d.fillRoundedRect(panelX.toFloat(), panelY.toFloat(), panelW.toFloat(), panelH.toFloat(), panelRadius)

        // 2. ヘッダー部分（少し明るい色で区切りを強調）
        val headerColor = themeScheme.getHoverColor(themeScheme.surfaceColor)
        g2d.fillStyle = headerColor
        // 上部だけ角丸にするためにパネル上部に重ねて描画
        g2d.fillRoundedRect(panelX.toFloat(), panelY.toFloat(), panelW.toFloat(), headerHeight.toFloat(), panelRadius)
        // 下側の角を消すための補完
        g2d.fillRect(panelX.toFloat(), panelY.toFloat() + headerHeight - panelRadius, panelW.toFloat(), panelRadius)

        // 3. アクセントライン（ヘッダーとコンテンツの境界）
        g2d.fillStyle = themeScheme.accentColor.alpha(120)
        g2d.fillRect(panelX.toFloat(), panelY.toFloat() + headerHeight, panelW.toFloat(), 1.5f)

        g2d.flush()

        // テキスト描画
        val font = minecraft.font
        val titleX = panelX + panelPadding
        val titleY = panelY + 10

        // タイトル
        guiGraphics.drawString(font, feature.name, titleX, titleY, themeScheme.foregroundColor, false)

        // 説明文（存在する場合）
        val descriptionKey = feature.translation()
        val description = Component.translatable(descriptionKey).string
        if (description != descriptionKey && description != feature.name) {
            val descY = titleY + font.lineHeight + 2
            guiGraphics.drawString(font, description, titleX, descY, themeScheme.secondaryColor, false)
        }

        super.render(guiGraphics, mouseX, mouseY, delta)
    }

    // --- インターフェース・イベントのブリッジ ---

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean = container.mouseClicked(mouseButtonEvent, bl) || super.mouseClicked(mouseButtonEvent, bl)

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean = container.mouseDragged(mouseButtonEvent, d, e) || super.mouseDragged(mouseButtonEvent, d, e)

    override fun mouseMoved(d: Double, e: Double) {
        container.mouseMoved(d, e)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean = container.mouseReleased(mouseButtonEvent) || super.mouseReleased(mouseButtonEvent)

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean = container.mouseScrolled(d, e, f, g) || super.mouseScrolled(d, e, f, g)

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(parent)
            return true
        }
        return container.keyPressed(keyEvent) || super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean = container.charTyped(characterEvent) || super.charTyped(characterEvent)

    override fun children(): List<GuiEventListener> = listOf(container)

    override fun onClose() {
        minecraft.setScreen(parent)
    }
}
