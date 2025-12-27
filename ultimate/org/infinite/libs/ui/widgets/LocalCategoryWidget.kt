package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.UltimateClient
import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.libs.ui.screen.GameScreen

class LocalCategoryWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val localCategory: LocalCategory,
    private val parent: GameScreen,
    private val thisIndex: Int,
) : AbstractWidget(x, y, width, height, Component.translatable(localCategory.translation())) {

    // 初期化時の時間を記録
    private val spawnTime = System.currentTimeMillis()
    private val animationDuration = 1000L // 1秒（ミリ秒）
    private val targetAlpha = 0.8f // 80%

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics, Minecraft.getInstance().deltaTracker)
        val theme = UltimateClient.theme

        // --- アニメーション計算 ---
        val elapsed = System.currentTimeMillis() - spawnTime
        val currentAlphaProgress = (elapsed / animationDuration.toFloat()).coerceAtMost(1.0f)
        val animatedAlpha = currentAlphaProgress * targetAlpha
        val screenWidth = graphics2DRenderer.width
        val screenHeight = graphics2DRenderer.height
        val width = (screenWidth * 0.5f)
            .coerceAtLeast(512f)
            .coerceAtMost(screenWidth * 0.9f)
        val height = screenHeight * 0.8f
        val x = (screenWidth - width) / 2f
        val y = (screenHeight - height) / 2f

        val foregroundColor = theme.colorScheme.foregroundColor
        theme.renderBackGround(x, y, width, height, graphics2DRenderer, animatedAlpha)
        graphics2DRenderer.fillStyle = foregroundColor
        graphics2DRenderer.textCentered(this.message.string, screenWidth / 2f, screenHeight / 2f)
        graphics2DRenderer.flush()
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }

    // 標準的な onClick オーバーライド
    override fun onClick(mouseButtonEvent: MouseButtonEvent, bl: Boolean) {
        println("Selected category: ${localCategory.translation()}")
        parent.pageIndex = thisIndex
    }
}
