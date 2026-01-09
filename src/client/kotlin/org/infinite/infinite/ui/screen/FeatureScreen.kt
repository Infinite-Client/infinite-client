package org.infinite.infinite.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.ui.layout.ScrollableLayoutContainer

class FeatureScreen<T : Feature>(
    private val feature: T,
    private val parent: Screen,
) : Screen(Component.translatable(feature.translation())) {

    private lateinit var container: ScrollableLayoutContainer

    // レイアウト定数
    private val headerHeight = 60
    private val margin = 10

    override fun init() {
        super.init()
        val innerWidth = width - (margin * 2)

        // 内部レイアウトの構築
        val innerLayout = LinearLayout.vertical().spacing(8)

        // TODO: Factoryを使用してプロパティウィジェットを追加
        // feature.properties.forEach { ... }

        innerLayout.arrangeElements()

        // スクロールコンテナの初期化
        container = ScrollableLayoutContainer(minecraft, innerLayout, innerWidth).apply {
            this.x = margin
            this.y = headerHeight
            this.setMinWidth(innerWidth)
            this.setMaxHeight(height - headerHeight - margin)
        }
        this.addRenderableWidget(container)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // 1. 背景の描画 (Vanilla)
        renderBackground(guiGraphics, mouseX, mouseY, delta)

        // 2. Graphics2DRenderer の初期化
        val g2d = Graphics2DRenderer(guiGraphics)
        val colorScheme = InfiniteClient.theme.colorScheme
        val centerX = width / 2f
        val size = 24f
        g2d.fillStyle = when (feature.featureType) {
            Feature.FeatureType.Cheat -> colorScheme.redColor
            Feature.FeatureType.Extend -> colorScheme.yellowColor
            Feature.FeatureType.Utils -> colorScheme.greenColor
        }
        g2d.textStyle.size = size
        g2d.textStyle.font = "infinite_regular"
        g2d.textStyle.shadow = true
        g2d.textCentered(feature.name, centerX, size)
        g2d.flush()
        // 4. ウィジェット（スクロールコンテナ等）の描画
        super.render(guiGraphics, mouseX, mouseY, delta)
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }
}
