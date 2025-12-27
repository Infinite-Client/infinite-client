package org.infinite.libs.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.infinite.UltimateClient
import org.infinite.libs.graphics.bundle.Graphics2DRenderer

class GameScreen : Screen(Component.literal("Ultimate Client")) {

    // 追加: Fabric API の初期化チェックを通すために必須
    override fun init() {
        super.init()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val currentColorScheme = UltimateClient.theme.colorScheme

        // 毎回 Minecraft.getInstance() を呼ぶより、
        // Screen クラスが持っている 'minecraft' フィールドを使うのが一般的です
        val graphics = Graphics2DRenderer(guiGraphics, minecraft!!.deltaTracker)

        graphics.fillStyle = currentColorScheme.color(60f, 0.5f, 0.5f, 1f)
        graphics.strokeRect(0f, 0f, 100f, 100f)
        graphics.render()

        // 最後に super を呼んで、ボタンなどの要素を描画
        super.render(guiGraphics, mouseX, mouseY, delta)
    }

    // 画面を閉じてもゲームがポーズされないようにする場合（必要に応じて）
    override fun isPauseScreen(): Boolean = false
}
