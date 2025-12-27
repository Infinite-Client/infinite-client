package org.infinite.libs.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.infinite.UltimateClient
import org.infinite.libs.ui.widgets.LocalCategoryWidget

class GameScreen : Screen(Component.literal("Ultimate Client")) {
    private var _pageIndex: Int = 0
    var pageIndex: Int
        get() = _pageIndex
        set(value) {
            _pageIndex = value % UltimateClient.localFeatures.categories.size
        }

    override fun init() {
        super.init() // 必須
        // 1. 幅の計算: 画面幅の50% か 812px の大きい方を選択
        // ただし、画面自体の幅(this.width)より大きくならないように調整
        val targetWidth = (this.width * 0.5).toInt().coerceAtLeast(812).coerceAtMost(this.width - 20)

        // 2. 高さの計算: 画面高さの90%
        val targetHeight = (this.height * 0.9).toInt()

        // 3. 中央配置のための座標計算
        val startX = (this.width - targetWidth) / 2
        val startY = (this.height - targetHeight) / 2

        // ウィジェットの配置（例として縦に並べる場合）
        val categories = UltimateClient.localFeatures.categories
        var currentY = startY + 10

        for ((index, entry) in categories.entries.withIndex()) {
            val category = entry.value
            // 引数に計算した座標や幅を渡すように LocalCategoryWidget を修正する必要があります
            this.addRenderableWidget(
                LocalCategoryWidget(
                    startX + 10,
                    currentY,
                    targetWidth - 20,
                    20, // x, y, width, height
                    category,
                    this,
                    index,
                ),
            )
            currentY += 25
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(guiGraphics, mouseX, mouseY, delta)
    }

    // 画面を閉じてもゲームがポーズされないようにする場合（必要に応じて）
    override fun isPauseScreen(): Boolean = false
}
